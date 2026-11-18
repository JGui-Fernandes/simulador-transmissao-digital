package br.unisinos.edu.graficos;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.graficos.BerAggregator.Curve;
import br.unisinos.edu.graficos.BerAggregator.Point;
import br.unisinos.edu.metricas.TheoreticalCurves;
import br.unisinos.edu.modulacao.Modulation;

/**
 * Gera as figuras da Etapa 11 a partir de um CSV de resultados.
 * Não roda simulações: apenas lê o CSV, agrega e desenha.
 */
public final class PlotGenerator {

    private static final int LARGURA = 900;
    private static final int ALTURA = 600;

    private static final String CANAL_AWGN = "AWGN";
    private static final String CODIGO_NENHUM = "NENHUM";
    private static final String CODIGO_HAMMING = "HAMMING74";

    private static final String PARAMETROS_RAJADAS = "rajadas: p = 0,01; r = 0,1; fator = 2";
    private static final String EIXO_X = "Eb/N0 (dB, por bit de informação)";
    private static final double PASSO_TEORICA = 0.1;

    private static final List<String> ORDEM_ESQUEMAS =
            List.of("BPSK", "QPSK", "QAM16", "AMI", "MANCHESTER");
    private static final Color[] CORES = {
            new Color(31, 119, 180),   // BPSK: azul
            new Color(255, 127, 14),   // QPSK: laranja
            new Color(44, 160, 44),    // 16-QAM: verde
            new Color(214, 39, 40),    // AMI: vermelho
            new Color(148, 103, 189),  // Manchester: roxo
            new Color(140, 86, 75)
    };

    private PlotGenerator() {
    }

    /**
     * Uso: PlotGenerator &lt;csv&gt; &lt;pasta_de_saida&gt;
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: PlotGenerator <csv> <pasta_de_saida>");
            System.exit(2);
        }
        List<Path> gerados = gerarTodas(Path.of(args[0]), Path.of(args[1]));
        for (Path f : gerados) {
            System.out.println("Gerado: " + f);
        }
    }

    /**
     * Gera as figuras e devolve os caminhos dos arquivos criados.
     * Figuras sem curvas correspondentes são omitidas, com aviso em stderr.
     */
    public static List<Path> gerarTodas(Path csv, Path pastaSaida) throws IOException {
        Objects.requireNonNull(csv, "csv não pode ser nulo");
        Objects.requireNonNull(pastaSaida, "pastaSaida não pode ser nula");
        Files.createDirectories(pastaSaida);

        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(ResultReader.read(csv));
        // Figuras do piloto recebem a marca de preliminar no título.
        String aviso = csv.getFileName().toString().contains("piloto") ? " [PRELIMINAR]" : "";

        List<Path> gerados = new ArrayList<>();

        // Figura 1: modulações em AWGN sem código, com curva teórica tracejada.
        gerarFigura(curvas,
                c -> ehModulacao(c.scheme()) && ehAwgn(c) && ehSemCodigo(c),
                "BER x Eb/N0 por modulação (AWGN, sem código)" + aviso,
                EIXO_X, pastaSaida, "fig1_modulacoes_awgn", true)
                .ifPresent(gerados::add);

        // Figura 2: AWGN × rajadas, sem código.
        gerarFigura(curvas,
                c -> ehModulacao(c.scheme()) && ehSemCodigo(c),
                "AWGN x rajadas (sem código; " + PARAMETROS_RAJADAS + ")" + aviso,
                EIXO_X, pastaSaida, "fig2_awgn_vs_rajadas", false)
                .ifPresent(gerados::add);

        // Figura 3: modulações em AWGN, com e sem Hamming(7,4).
        gerarFigura(curvas,
                c -> ehModulacao(c.scheme()) && ehAwgn(c),
                "Com e sem Hamming(7,4), modulações (AWGN)" + aviso,
                EIXO_X, pastaSaida, "fig3_hamming_modulacoes", false)
                .ifPresent(gerados::add);

        // Figura 4: códigos de linha em AWGN, com e sem Hamming(7,4). Teóricas de AMI e Manchester.
        gerarFigura(curvas,
                c -> ehLinha(c.scheme()) && ehAwgn(c),
                "Com e sem Hamming(7,4), códigos de linha (AWGN)" + aviso,
                EIXO_X, pastaSaida, "fig4_hamming_linhas", true)
                .ifPresent(gerados::add);

        return gerados;
    }

    private static Optional<Path> gerarFigura(Map<Curve, List<Point>> curvas, Predicate<Curve> filtro,
                                              String titulo, String eixoX, Path pastaSaida,
                                              String nomeBase, boolean comTeoria) throws IOException {

        List<Map.Entry<Curve, List<Point>>> selecionadas = curvas.entrySet().stream()
                .filter(e -> filtro.test(e.getKey()) && !e.getValue().isEmpty())
                .toList();
        if (selecionadas.isEmpty()) {
            System.err.println("Figura omitida (sem curvas): " + nomeBase);
            return Optional.empty();
        }

        long n = selecionadas.get(0).getValue().get(0).messageBits();

        XYChart chart = new XYChartBuilder()
                .width(LARGURA)
                .height(ALTURA)
                .title(titulo + " - piso 1/N, N = " + n + " bits por semente")
                .xAxisTitle(eixoX)
                .yAxisTitle("BER")
                .build();
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);

        verificarNomesUnicos(selecionadas, nomeBase);

        List<Double> pisoX = new ArrayList<>();
        List<Double> pisoY = new ArrayList<>();

        for (Map.Entry<Curve, List<Point>> entrada : selecionadas) {
            Curve curva = entrada.getKey();
            List<Point> pontos = entrada.getValue();

            double[] x = new double[pontos.size()];
            double[] y = new double[pontos.size()];
            double[] erro = new double[pontos.size()];
            for (int k = 0; k < pontos.size(); k++) {
                Point p = pontos.get(k);
                double media = p.ber().media();
                x[k] = p.ebn0Db();
                y[k] = media;
                // Limite: a barra não pode descer abaixo do piso 1/N (escala log exige valor positivo).
                // O corte é simétrico, então também encurta a parte superior. Registrar no relatório.
                erro[k] = Math.min(p.ber().meiaLargura(), media - 1.0 / p.messageBits());

                if (p.temPiso()) {
                    pisoX.add(p.ebn0Db());
                    pisoY.add(media);
                }
            }

            boolean variante = ehVariante(curva);
            Color cor = corDoEsquema(curva.scheme());

            XYSeries serie = chart.addSeries(nomeDaCurva(curva), x, y, erro);
            serie.setLineColor(cor);
            serie.setMarkerColor(cor);
            serie.setMarker(variante ? SeriesMarkers.SQUARE : SeriesMarkers.CIRCLE);
            serie.setLineStyle(variante ? SeriesLines.DASH_DOT : SeriesLines.SOLID);
        }

        // Marcadores sobre os pontos que dependem do piso, em uma série só, para aparecer uma vez na legenda.
        if (!pisoX.isEmpty()) {
            adicionarMarcadoresPiso(chart, pisoX, pisoY);
        }

        // Linha do limite de detecção em todas as figuras: o piso aparece em todas.
        adicionarLimiteDeteccao(chart, selecionadas, n);

        if (comTeoria) {
            adicionarTeoricas(chart, selecionadas);
        }

        // BitmapEncoder acrescenta a extensão .png sozinho quando recebe o nome sem ela.
        BitmapEncoder.saveBitmap(chart, pastaSaida.resolve(nomeBase).toString(), BitmapFormat.PNG);
        return Optional.of(pastaSaida.resolve(nomeBase + ".png"));
    }

    /**
     * Linha horizontal em 1/N: menor BER que a simulação consegue medir com N bits por semente.
     */
    private static void adicionarLimiteDeteccao(XYChart chart,
                                                List<Map.Entry<Curve, List<Point>>> selecionadas,
                                                long n) {
        double[] faixa = intervaloEbn0(selecionadas);
        double piso = 1.0 / n;

        XYSeries s = chart.addSeries("Limite de detecção (1/N)",
                new double[]{faixa[0], faixa[1]},
                new double[]{piso, piso});
        s.setLineColor(Color.GRAY);
        s.setLineStyle(SeriesLines.DOT_DOT);
        s.setMarker(SeriesMarkers.NONE);
    }

    /**
     * Losangos cinzas, sem linha, sobre os pontos em que pelo menos uma semente teve zero erros.
     * Nesses pontos o valor plotado é limitado pelo piso 1/N, não por uma medição.
     */
    private static void adicionarMarcadoresPiso(XYChart chart, List<Double> xs, List<Double> ys) {
        double[] x = xs.stream().mapToDouble(Double::doubleValue).toArray();
        double[] y = ys.stream().mapToDouble(Double::doubleValue).toArray();

        XYSeries s = chart.addSeries("Ponto com piso 1/N (0 erros em alguma semente)", x, y);
        s.setLineStyle(SeriesLines.NONE);
        s.setMarker(SeriesMarkers.DIAMOND);
        s.setMarkerColor(Color.DARK_GRAY);
    }

    /**
     * Desenha uma linha teórica por família. O rótulo é formado pelos esquemas
     * efetivamente presentes na figura: a Figura 1 mostra "Teórica BPSK/QPSK" e a
     * Figura 4 mostra "Teórica Manchester". Usa só as curvas sem código em AWGN.
     */
    private static void adicionarTeoricas(XYChart chart, List<Map.Entry<Curve, List<Point>>> selecionadas) {
        Map<String, List<String>> rotulosPorFamilia = new LinkedHashMap<>();

        for (Map.Entry<Curve, List<Point>> entrada : selecionadas) {
            Curve curva = entrada.getKey();
            if (ehAwgn(curva) && ehSemCodigo(curva)) {
                String familia = familiaTeorica(curva.scheme()).orElse(null);
                if (familia != null) {
                    rotulosPorFamilia.computeIfAbsent(familia, k -> new ArrayList<>())
                            .add(rotulo(curva.scheme()));
                }
            }
        }

        double[] faixa = intervaloEbn0(selecionadas);
        int m = (int) Math.round((faixa[1] - faixa[0]) / PASSO_TEORICA) + 1;
        double[] xs = new double[m];
        for (int i = 0; i < m; i++) {
            xs[i] = faixa[0] + i * PASSO_TEORICA;
        }

        for (Map.Entry<String, List<String>> f : rotulosPorFamilia.entrySet()) {
            double[] ys = new double[m];
            for (int i = 0; i < m; i++) {
                ys[i] = teorica(f.getKey(), xs[i]);
            }
            String nome = "Teórica " + String.join("/", f.getValue())
                    + (f.getKey().equals("16-QAM") ? " (aproximação)" : "");
            XYSeries t = chart.addSeries(nome, xs, ys);
            t.setLineColor(corTeorica(f.getKey()));
            t.setLineStyle(SeriesLines.DASH_DASH);
            t.setMarker(SeriesMarkers.NONE);
        }
    }

    /** Menor e maior Eb/N0 presentes nas curvas selecionadas, como {min, max}. */
    private static double[] intervaloEbn0(List<Map.Entry<Curve, List<Point>>> selecionadas) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Curve, List<Point>> entrada : selecionadas) {
            for (Point p : entrada.getValue()) {
                min = Math.min(min, p.ebn0Db());
                max = Math.max(max, p.ebn0Db());
            }
        }
        return new double[]{min, max};
    }

    /** BPSK/QPSK/Manchester em preto; 16-QAM na cor do esquema; AMI em cinza para não se confundir com o AMI com Hamming. */
    private static Color corTeorica(String familia) {
        return switch (familia) {
            case "16-QAM" -> corDoEsquema("QAM16");
            case "AMI" -> Color.GRAY;
            default -> Color.BLACK;
        };
    }

    private static void verificarNomesUnicos(List<Map.Entry<Curve, List<Point>>> selecionadas, String nomeBase) {
        Set<String> nomes = new HashSet<>();
        for (Map.Entry<Curve, List<Point>> entrada : selecionadas) {
            String nome = nomeDaCurva(entrada.getKey());
            if (!nomes.add(nome)) {
                throw new IllegalStateException("Nome de série duplicado na figura " + nomeBase + ": " + nome);
            }
        }
    }

    private static String nomeDaCurva(Curve curva) {
        return rotulo(curva.scheme()) + " - " + curva.channel() + " - " + rotuloCodigo(curva.code());
    }

    /**
     * Rótulo legível do código de canal. Valores desconhecidos aparecem como estão no CSV,
     * para que o nome da série nunca colida com outro.
     */
    private static String rotuloCodigo(String codigo) {
        return switch (codigo) {
            case CODIGO_NENHUM -> "sem código";
            case CODIGO_HAMMING -> "Hamming(7,4)";
            default -> codigo;
        };
    }

    private static String rotulo(String esquema) {
        if (ehModulacao(esquema)) {
            return Modulation.valueOf(esquema).rotulo();
        }
        if (ehLinha(esquema)) {
            return LineCode.valueOf(esquema).rotulo();
        }
        return esquema;
    }

    /** Linha tracejada e marcador quadrado para rajadas ou Hamming; sólida e circular para o básico. */
    private static boolean ehVariante(Curve curva) {
        return !ehAwgn(curva) || !ehSemCodigo(curva);
    }

    private static Color corDoEsquema(String esquema) {
        int i = ORDEM_ESQUEMAS.indexOf(esquema);
        return CORES[(i < 0 ? 0 : i) % CORES.length];
    }

    private static boolean ehAwgn(Curve curva) {
        return curva.channel().equals(CANAL_AWGN);
    }

    private static boolean ehSemCodigo(Curve curva) {
        return curva.code().equals(CODIGO_NENHUM);
    }

    private static boolean ehModulacao(String esquema) {
        return Arrays.stream(Modulation.values()).anyMatch(m -> m.name().equals(esquema));
    }

    private static boolean ehLinha(String esquema) {
        return Arrays.stream(LineCode.values()).anyMatch(l -> l.name().equals(esquema));
    }

    /** Família teórica do esquema, ou vazio se não houver fórmula conhecida. */
    private static Optional<String> familiaTeorica(String esquema) {
        return switch (esquema) {
            case "BPSK", "QPSK", "MANCHESTER" -> Optional.of("BPSK/QPSK/Manchester");
            case "QAM16" -> Optional.of("16-QAM");
            case "AMI" -> Optional.of("AMI");
            default -> Optional.empty();
        };
    }

    /** BER teórica da família, em função de Eb/N0 em dB. */
    private static double teorica(String familia, double ebn0Db) {
        return switch (familia) {
            case "BPSK/QPSK/Manchester" -> TheoreticalCurves.berBpskQpsk(ebn0Db);
            case "16-QAM" -> TheoreticalCurves.ber16Qam(ebn0Db);
            case "AMI" -> berAmi(ebn0Db);
            default -> throw new IllegalArgumentException("família teórica desconhecida: " + familia);
        };
    }

    private static double berAmi(double ebn0Db) {
        return 1.5 * TheoreticalCurves.berBpskQpsk(ebn0Db - 10 * Math.log10(2));
    }
}