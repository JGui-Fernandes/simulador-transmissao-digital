package br.unisinos.edu.graficos;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.graficos.BerAggregator.Curve;
import br.unisinos.edu.graficos.BerAggregator.Point;
import br.unisinos.edu.metricas.TheoreticalCurves;
import br.unisinos.edu.modulacao.Modulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

/**
 * Gera a tabela de compromisso da Etapa 12 a partir do CSV de resultados:
 * eficiência espectral nominal e Eb/N0 necessário para cada BER-alvo.
 * Não roda simulações: lê o CSV, agrega com o BerAggregator e interpola
 * linearmente em log10(BER) entre os pontos simulados.
 *
 * Uso: TradeoffTable [csv saida.md]
 * Sem argumentos, usa o maior CSV de resultados/brutos (ignorando arquivos "piloto")
 * e grava em docs/tabela_compromisso.md.
 */
public final class TradeoffTable {

    /** BERs-alvo. Com 10⁶ bits por semente, 10⁻⁴ ainda corresponde a ≈ 100 erros por execução. */
    static final double[] ALVOS = {1e-3, 1e-4};

    private static final Path PASTA_CSV = Path.of("resultados", "brutos");
    private static final Path SAIDA_PADRAO = Path.of("docs", "tabela_compromisso.md");

    private static final List<String> ORDEM_ESQUEMAS = List.of("BPSK", "QPSK", "QAM16", "AMI", "MANCHESTER");
    private static final List<String> ORDEM_CANAIS = List.of("AWGN", "RAJADAS");
    private static final List<String> ORDEM_CODIGOS = List.of("NENHUM", "HAMMING74");
    private static final double TAXA_HAMMING = 4.0 / 7.0;

    /** Onde a curva cruza a BER-alvo; comPiso indica que um dos pontos usados dependeu do piso 1/N. */
    record Cruzamento(double ebn0Db, boolean comPiso) {
    }

    private TradeoffTable() {
    }

    public static void main(String[] args) throws IOException {
        Path csv;
        Path saida;
        if (args.length == 2) {
            csv = Path.of(args[0]);
            saida = Path.of(args[1]);
        } else if (args.length == 0) {
            csv = encontrarCsv(PASTA_CSV);
            saida = SAIDA_PADRAO;
        } else {
            System.err.println("Uso: TradeoffTable [csv saida.md]");
            System.exit(2);
            return;
        }

        System.out.println("Lendo: " + csv.toAbsolutePath());
        String md = gerar(BerAggregator.aggregate(ResultReader.read(csv)));

        Path pai = saida.toAbsolutePath().getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }
        Files.writeString(saida, md, StandardCharsets.UTF_8);
        System.out.println("Gerado: " + saida.toAbsolutePath());
    }

    /** Maior CSV da pasta, ignorando arquivos com "piloto" no nome. */
    static Path encontrarCsv(Path pasta) throws IOException {
        if (!Files.isDirectory(pasta)) {
            throw new IllegalStateException("Pasta não encontrada: " + pasta.toAbsolutePath()
                    + ". Confira se o Working directory da execução é a pasta do projeto.");
        }
        try (Stream<Path> arquivos = Files.list(pasta)) {
            return arquivos
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .filter(p -> !p.getFileName().toString().toLowerCase(Locale.ROOT).contains("piloto"))
                    .max(Comparator.comparingLong(TradeoffTable::tamanho))
                    .orElseThrow(() -> new IllegalStateException(
                            "Nenhum CSV de resultados em " + pasta.toAbsolutePath()));
        }
    }

    private static long tamanho(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Monta o Markdown completo da tabela de compromisso. */
    public static String gerar(Map<Curve, List<Point>> curvas) {
        Objects.requireNonNull(curvas, "curvas não pode ser nulo");

        List<Curve> ordenadas = new ArrayList<>(curvas.keySet());
        ordenadas.sort(Comparator
                .comparingInt((Curve c) -> indice(ORDEM_ESQUEMAS, c.scheme()))
                .thenComparingInt(c -> indice(ORDEM_CANAIS, c.channel()))
                .thenComparingInt(c -> indice(ORDEM_CODIGOS, c.code())));

        StringBuilder sb = new StringBuilder();
        sb.append("# Tabela de compromisso\n\n");
        sb.append("Gerada por `TradeoffTable` a partir do CSV de resultados. ")
                .append("Eb/N0 em dB, por bit de informação. Os valores simulados são interpolados ")
                .append("linearmente em log10(BER) entre os pontos medidos (passo de 2 dB). ")
                .append("\"> X\": a BER-alvo não foi atingida até o maior Eb/N0 simulado. ")
                .append("\"< X\": já estava abaixo dela no menor. ")
                .append("A coluna teórica existe só para AWGN sem código.\n\n");

        sb.append("| Esquema | Canal | Código | Eficiência nominal (bit/s/Hz)");
        for (double alvo : ALVOS) {
            sb.append(" | Eb/N0 p/ BER ").append(potencia(alvo)).append(" (simulado) | (teoria)");
        }
        sb.append(" |\n|---|---|---|---");
        for (int i = 0; i < ALVOS.length; i++) {
            sb.append("|---|---");
        }
        sb.append("|\n");

        for (Curve c : ordenadas) {
            List<Point> pontos = curvas.get(c);
            sb.append("| ").append(rotulo(c.scheme()))
                    .append(" | ").append(c.channel())
                    .append(" | ").append(c.code().equals("HAMMING74") ? "Hamming(7,4)" : "nenhum")
                    .append(" | ").append(numero(eficiencia(c), 3));
            DoubleUnaryOperator teoria = ehAwgnSemCodigo(c) ? teoria(c.scheme()) : null;
            for (double alvo : ALVOS) {
                sb.append(" | ").append(textoSimulado(pontos, alvo));
                sb.append(" | ").append(teoria == null ? "—" : numero(ebn0Teorico(teoria, alvo), 1));
            }
            sb.append(" |\n");
        }
        sb.append("\n\\* Um dos pontos usados na interpolação teve 0 erros em alguma semente ")
                .append("(entrou na média com o piso 1/N).\n");

        double alvo = ALVOS[0];
        sb.append("\n## Diferenças em dB para BER ").append(potencia(alvo)).append("\n\n");
        sb.append("Positivo: precisa de mais Eb/N0 (piora). Negativo: precisa de menos (ganho). ")
                .append("\"—\" quando algum dos dois valores não foi atingido na faixa simulada.\n\n");
        sb.append("| Esquema | Hamming − sem código (AWGN) | Hamming − sem código (rajadas) ")
                .append("| Rajadas − AWGN (sem código) |\n|---|---|---|---|\n");
        for (String esquema : ORDEM_ESQUEMAS) {
            boolean presente = curvas.keySet().stream().anyMatch(c -> c.scheme().equals(esquema));
            if (!presente) {
                continue;
            }
            sb.append("| ").append(rotulo(esquema))
                    .append(" | ").append(diferenca(curvas,
                            new Curve(esquema, "AWGN", "HAMMING74"), new Curve(esquema, "AWGN", "NENHUM"), alvo))
                    .append(" | ").append(diferenca(curvas,
                            new Curve(esquema, "RAJADAS", "HAMMING74"), new Curve(esquema, "RAJADAS", "NENHUM"), alvo))
                    .append(" | ").append(diferenca(curvas,
                            new Curve(esquema, "RAJADAS", "NENHUM"), new Curve(esquema, "AWGN", "NENHUM"), alvo))
                    .append(" |\n");
        }
        return sb.toString();
    }

    /** Eb/N0 em que a média simulada cruza a BER-alvo, interpolando em log10(BER). */
    static Optional<Cruzamento> cruzamento(List<Point> pontos, double alvo) {
        for (int i = 0; i + 1 < pontos.size(); i++) {
            Point a = pontos.get(i);
            Point b = pontos.get(i + 1);
            double ya = a.ber().media();
            double yb = b.ber().media();
            if (ya >= alvo && yb <= alvo) {
                double la = Math.log10(ya);
                double lb = Math.log10(yb);
                double lt = Math.log10(alvo);
                double x = la == lb
                        ? a.ebn0Db()
                        : a.ebn0Db() + (la - lt) / (la - lb) * (b.ebn0Db() - a.ebn0Db());
                return Optional.of(new Cruzamento(x, a.temPiso() || b.temPiso()));
            }
        }
        return Optional.empty();
    }

    static String textoSimulado(List<Point> pontos, double alvo) {
        if (pontos.isEmpty()) {
            return "—";
        }
        Point primeiro = pontos.get(0);
        if (primeiro.ber().media() < alvo) {
            return "< " + numero(primeiro.ebn0Db(), 0);
        }
        return cruzamento(pontos, alvo)
                .map(c -> numero(c.ebn0Db(), 1) + (c.comPiso() ? " *" : ""))
                .orElse("> " + numero(pontos.get(pontos.size() - 1).ebn0Db(), 0));
    }

    /** Eb/N0 teórico (dB) para a BER-alvo, por bisseção (a BER cai com o Eb/N0). */
    static double ebn0Teorico(DoubleUnaryOperator ber, double alvo) {
        double baixo = -10.0;
        double alto = 30.0;
        for (int i = 0; i < 100; i++) {
            double meio = (baixo + alto) / 2.0;
            if (ber.applyAsDouble(meio) > alvo) {
                baixo = meio;
            } else {
                alto = meio;
            }
        }
        return (baixo + alto) / 2.0;
    }

    /** Curva teórica do esquema em AWGN sem código, ou null se não houver fórmula. */
    static DoubleUnaryOperator teoria(String esquema) {
        return switch (esquema) {
            case "BPSK", "QPSK", "MANCHESTER" -> TheoreticalCurves::berBpskQpsk;
            case "QAM16" -> TheoreticalCurves::ber16Qam;
            case "AMI" -> db -> 1.5 * TheoreticalCurves.berBpskQpsk(db - 10 * Math.log10(2));
            default -> null;
        };
    }

    /** Eficiência espectral nominal em bit/s/Hz, já multiplicada pela taxa do código. */
    static double eficiencia(Curve c) {
        double base;
        if (Arrays.stream(Modulation.values()).anyMatch(m -> m.name().equals(c.scheme()))) {
            base = Modulation.valueOf(c.scheme()).eficienciaEspectral();
        } else if (Arrays.stream(LineCode.values()).anyMatch(l -> l.name().equals(c.scheme()))) {
            base = LineCode.valueOf(c.scheme()).eficienciaEspectral();
        } else {
            return Double.NaN;
        }
        return c.code().equals("HAMMING74") ? base * TAXA_HAMMING : base;
    }

    private static String diferenca(Map<Curve, List<Point>> curvas, Curve a, Curve b, double alvo) {
        Optional<Double> va = valor(curvas, a, alvo);
        Optional<Double> vb = valor(curvas, b, alvo);
        if (va.isEmpty() || vb.isEmpty()) {
            return "—";
        }
        double d = va.get() - vb.get();
        return (d > 0 ? "+" : "") + numero(d, 1);
    }

    private static Optional<Double> valor(Map<Curve, List<Point>> curvas, Curve c, double alvo) {
        List<Point> pontos = curvas.get(c);
        if (pontos == null || pontos.isEmpty() || pontos.get(0).ber().media() < alvo) {
            return Optional.empty();
        }
        return cruzamento(pontos, alvo).map(Cruzamento::ebn0Db);
    }

    private static boolean ehAwgnSemCodigo(Curve c) {
        return c.channel().equals("AWGN") && c.code().equals("NENHUM");
    }

    private static String rotulo(String esquema) {
        if (Arrays.stream(Modulation.values()).anyMatch(m -> m.name().equals(esquema))) {
            return Modulation.valueOf(esquema).rotulo();
        }
        if (Arrays.stream(LineCode.values()).anyMatch(l -> l.name().equals(esquema))) {
            return LineCode.valueOf(esquema).rotulo();
        }
        return esquema;
    }

    private static int indice(List<String> ordem, String valor) {
        int i = ordem.indexOf(valor);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** Número com vírgula decimal, sem depender do locale da máquina. */
    static String numero(double v, int casas) {
        if (!Double.isFinite(v)) {
            return "—";
        }
        String s = String.format(Locale.ROOT, "%." + casas + "f", v).replace('.', ',');
        return s.matches("-0(,0+)?") ? s.substring(1) : s;
    }

    /** 1e-3 vira "10⁻³". */
    static String potencia(double alvo) {
        String sobrescritos = "⁰¹²³⁴⁵⁶⁷⁸⁹";
        int expoente = (int) Math.round(Math.log10(alvo));
        StringBuilder sb = new StringBuilder("10");
        if (expoente < 0) {
            sb.append('⁻');
        }
        for (char ch : Integer.toString(Math.abs(expoente)).toCharArray()) {
            sb.append(sobrescritos.charAt(ch - '0'));
        }
        return sb.toString();
    }
}