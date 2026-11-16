package br.unisinos.edu.experimento;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static ExperimentoConfig carregar(Path arquivo) throws IOException {
        Properties props = new Properties();
        try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            props.load(leitor);
        }
        return parse(props);
    }

    public static ExperimentoConfig parse(Properties props) {
        List<Double> ebn0 = lista(props, "ebn0.valores").stream()
                .map(v -> real("ebn0.valores", v))
                .toList();
        long bitsLido = inteiro(props, "bits");
        if (bitsLido <= 0 || bitsLido > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("bits fora do intervalo permitido: " + bitsLido);
        }
        int bits = (int) bitsLido;
        List<Long> sementes = lista(props, "sementes").stream()
                .map(v -> inteiro("sementes", v))
                .toList();
        List<Modulation> modulacoes = lista(props, "modulacoes").stream()
                .map(v -> enumDe(Modulation.class, "modulacoes", v))
                .toList();
        List<LineCode> linhas = lista(props, "linhas").stream()
                .map(v -> enumDe(LineCode.class, "linhas", v))
                .toList();
        List<ChannelType> canais = lista(props, "canais").stream()
                .map(v -> enumDe(ChannelType.class, "canais", v))
                .toList();
        List<Boolean> codigos = lista(props, "codigos").stream()
                .map(ConfigLoader::codigo)
                .toList();

        double p = realOpcional(props, "rajadas.p", ConfigTransmission.P_PADRAO);
        double r = realOpcional(props, "rajadas.r", ConfigTransmission.R_PADRAO);
        double fator = realOpcional(props, "rajadas.fator", ConfigTransmission.FATOR_PADRAO);

        return new ExperimentoConfig(ebn0, bits, sementes, modulacoes, linhas,
                canais, codigos, p, r, fator);
    }

    private static List<String> lista(Properties props, String chave) {
        String valor = obrigatorio(props, chave);

        if (valor.isBlank()) {
            return List.of();
        }

        String[] partes = valor.split(",", -1);
        for (String parte : partes) {
            if (parte.isBlank()) {
                throw new IllegalArgumentException("item vazio em " + chave + ": " + valor);
            }
        }
        return Arrays.stream(partes).map(String::trim).toList();
    }

    private static String obrigatorio(Properties props, String chave) {
        String valor = props.getProperty(chave);
        if (valor == null) {
            throw new IllegalArgumentException("chave ausente na configuração: " + chave);
        }
        return valor;
    }

    private static <E extends Enum<E>> E enumDe(Class<E> tipo, String chave, String valor) {
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("valor inválido em " + chave + ": " + valor
                    + " (esperado um de " + Arrays.toString(tipo.getEnumConstants()) + ")", e);
        }
    }

    private static boolean codigo(String valor) {
        return switch (valor.trim().toUpperCase(Locale.ROOT)) {
            case "NENHUM" -> false;
            case "HAMMING74" -> true;
            default -> throw new IllegalArgumentException(
                    "código de canal desconhecido em 'codigos': " + valor);
        };
    }

    private static double real(String chave, String valor) {
        double v;
        try {
            v = Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("número inválido em " + chave + ": " + valor, e);
        }
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("valor não finito em " + chave + ": " + valor);
        }
        return v;
    }

    private static long inteiro(String chave, String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("inteiro inválido em " + chave + ": " + valor, e);
        }
    }

    private static long inteiro(Properties props, String chave) {
        return inteiro(chave, obrigatorio(props, chave));
    }

    private static double realOpcional(Properties props, String chave, double padrao) {
        String valor = props.getProperty(chave);
        return valor == null ? padrao : real(chave, valor);
    }


}