package br.unisinos.edu.cli;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Mode;
import br.unisinos.edu.transmissao.TransmissionResult;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.Transmission;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Cli {

    public static final int SUCESSO = 0;
    public static final int ERRO_DE_USO = 2;

    private static final String MENSAGEM_PADRAO = "Redes de Computadores - GB 2026!";

    private static final Set<String> OPCOES = Set.of(
            "mensagem", "modo", "linha", "modulacao", "codigo", "canal",
            "ebn0", "semente", "p", "r", "fator", "help");

    private static final String USO = """
            Uso: java -jar gb.jar [opções]

              --mensagem  texto ASCII a transmitir            (padrão: "%s")
              --modo      LINHA | MODULADO                    (padrão: MODULADO)
              --linha     AMI | MANCHESTER                    (padrão: MANCHESTER; só no modo LINHA)
              --modulacao BPSK | QPSK | QAM16                 (padrão: QPSK; só no modo MODULADO)
              --codigo    NENHUM | HAMMING74                  (padrão: NENHUM)
              --canal     AWGN | RAJADAS                      (padrão: AWGN)
              --ebn0      Eb/N0 em dB                         (padrão: 10)
              --semente   semente do ruído                    (padrão: 42)
              --p         rajadas: probabilidade de entrar no estado ruim  (padrão: %s)
              --r         rajadas: probabilidade de sair do estado ruim    (padrão: %s)
              --fator     rajadas: fator de degradação do sigma            (padrão: %s)
              --help      mostra esta mensagem

            Exemplo:
              java -jar gb.jar --mensagem "Redes" --modo MODULADO --modulacao QAM16 --ebn0 10 --canal AWGN --semente 42
            """;

    private Cli() {
    }

    /** @return 0 em caso de sucesso, 2 para erro de uso (opção ou valor inválido). */
    public static int executar(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args não pode ser nulo");
        Objects.requireNonNull(out, "out não pode ser nulo");
        Objects.requireNonNull(err, "err não pode ser nulo");
        try {
            Map<String, String> opcoes = lerOpcoes(args);
            if (opcoes.containsKey("help")) {
                out.print(uso());
                return SUCESSO;
            }

            String mensagem = opcoes.getOrDefault("mensagem", MENSAGEM_PADRAO);
            if (mensagem.isEmpty()) {
                throw new IllegalArgumentException("a mensagem não pode ser vazia");
            }
            ConfigTransmission config = montarConfig(opcoes);

            int[] bits = DataGenerator.textoParaBits(mensagem);
            TransmissionResult r = Transmission.executar(bits, config);
            imprimir(out, mensagem, r);
            return SUCESSO;
        } catch (IllegalArgumentException e) {
            err.println("Erro: " + e.getMessage());
            err.println("Use --help para ver as opções.");
            return ERRO_DE_USO;
        }
    }

    public static String uso() {
        return USO.formatted(MENSAGEM_PADRAO, ConfigTransmission.P_PADRAO,
                ConfigTransmission.R_PADRAO, ConfigTransmission.FATOR_PADRAO);
    }

    /** Troca caracteres não imprimíveis por \xNN, porque bits errados geram qualquer byte. */
    public static String textoSeguro(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(String.format("\\x%02X", (int) c));
            }
        }
        return sb.toString();
    }

    // ---------- leitura dos argumentos ----------

    private static Map<String, String> lerOpcoes(String[] args) {
        Map<String, String> opcoes = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("argumento inesperado: " + arg);
            }
            String nome = arg.substring(2);
            if (!OPCOES.contains(nome)) {
                throw new IllegalArgumentException("opção desconhecida: " + arg);
            }
            if (opcoes.containsKey(nome)) {
                throw new IllegalArgumentException("opção repetida: " + arg);
            }
            if (nome.equals("help")) {
                opcoes.put(nome, "");
                continue;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("falta o valor de " + arg);
            }
            opcoes.put(nome, args[++i]);
        }
        return opcoes;
    }

    private static ConfigTransmission montarConfig(Map<String, String> opcoes) {
        Mode modo = lerEnum(opcoes, "modo", Mode.MODULADO);
        ChannelType canal = lerEnum(opcoes, "canal", ChannelType.AWGN);
        boolean hamming = lerCodigo(opcoes);
        double ebn0 = lerDouble(opcoes, "ebn0", 10.0);
        long semente = lerLong(opcoes, "semente", 42L);

        ConfigTransmission base;
        if (modo == Mode.LINHA) {
            base = ConfigTransmission.paraLinha(
                    lerEnum(opcoes, "linha", LineCode.MANCHESTER), hamming, canal, ebn0, semente);
        } else {
            base = ConfigTransmission.paraModulacao(
                    lerModulacao(opcoes), hamming, canal, ebn0, semente);
        }
        return base.comRajadas(
                lerDouble(opcoes, "p", ConfigTransmission.P_PADRAO),
                lerDouble(opcoes, "r", ConfigTransmission.R_PADRAO),
                lerDouble(opcoes, "fator", ConfigTransmission.FATOR_PADRAO));
    }

    private static String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
    }

    private static <E extends Enum<E>> E lerEnum(Map<String, String> opcoes, String nome, E padrao) {
        String valor = opcoes.get(nome);
        if (valor == null) {
            return padrao;
        }
        for (E e : padrao.getDeclaringClass().getEnumConstants()) {
            if (normalizar(e.name()).equals(normalizar(valor))) {
                return e;
            }
        }
        throw new IllegalArgumentException("valor inválido para --" + nome + ": '" + valor + "'");
    }

    private static Modulation lerModulacao(Map<String, String> opcoes) {
        String valor = opcoes.get("modulacao");
        if (valor == null) {
            return Modulation.QPSK;
        }
        String n = normalizar(valor);
        if (n.equals("16QAM")) {
            n = "QAM16";
        }
        for (Modulation m : Modulation.values()) {
            if (normalizar(m.name()).equals(n)) {
                return m;
            }
        }
        throw new IllegalArgumentException("valor inválido para --modulacao: '" + valor + "'");
    }

    private static boolean lerCodigo(Map<String, String> opcoes) {
        String valor = opcoes.get("codigo");
        if (valor == null) {
            return false;
        }
        String n = normalizar(valor);
        if (n.equals("NENHUM")) {
            return false;
        }
        if (n.equals("HAMMING74") || n.equals("HAMMING")) {
            return true;
        }
        throw new IllegalArgumentException("valor inválido para --codigo: '" + valor + "'");
    }

    private static double lerDouble(Map<String, String> opcoes, String nome, double padrao) {
        String valor = opcoes.get(nome);
        if (valor == null) {
            return padrao;
        }
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido para --" + nome + ": '" + valor + "'");
        }
    }

    private static long lerLong(Map<String, String> opcoes, String nome, long padrao) {
        String valor = opcoes.get(nome);
        if (valor == null) {
            return padrao;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido para --" + nome + ": '" + valor + "'");
        }
    }

    // ---------- saída ----------

    private static void imprimir(PrintStream out, String mensagem, TransmissionResult r) {
        ConfigTransmission c = r.config();
        String recebida = DataGenerator.bitsParaTexto(r.bitsRecebidos());

        String descricaoModo = c.modo() == Mode.LINHA
                ? "LINHA (" + c.linha().rotulo() + ")"
                : "MODULADO (" + c.modulacao().rotulo() + ")";
        String descricaoCanal = c.canal() == ChannelType.RAJADAS
                ? String.format(Locale.ROOT, "rajadas (p=%s, r=%s, fator=%s)", c.p(), c.r(), c.fatorDegradacao())
                : "AWGN";
        String descricaoCodigo = c.hamming()
                ? String.format(Locale.ROOT, "Hamming(7,4), R = %.4f", r.taxaCodigo())
                : "nenhum";

        out.println("Modo: " + descricaoModo);
        out.println("Código de canal: " + descricaoCodigo);
        out.println("Canal: " + descricaoCanal);
        out.println(String.format(Locale.ROOT, "Eb/N0: %.2f dB", c.ebn0Db()));
        out.println("Semente: " + c.semente());
        out.println("Mensagem enviada: " + textoSeguro(mensagem));
        out.println("Mensagem recebida: " + textoSeguro(recebida));
        out.println("Bits da mensagem: " + r.ber().totalBits());
        out.println("Bits transmitidos no canal: " + r.bitsTransmitidos());
        out.println("Bits errados: " + r.ber().bitsErrados());
        out.println(String.format(Locale.ROOT, "BER: %.6e", r.ber().taxa()));
        out.println("Posições dos bits errados: " + r.ber().posicoesErradas());
        out.println("Mensagem recuperada: " + (r.mensagemRecuperada() ? "sim" : "não"));
        out.println(String.format(Locale.ROOT, "Eficiência espectral nominal: %.2f bits/s/Hz",
                r.eficienciaEspectral()));
    }
}