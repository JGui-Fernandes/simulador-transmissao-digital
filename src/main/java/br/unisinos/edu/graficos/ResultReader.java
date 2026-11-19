package br.unisinos.edu.graficos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lê o CSV bruto gravado pelo {@code ResultWriter}. As colunas são localizadas pelo nome
 * do cabeçalho, então a ordem delas não importa. Cada linha do CSV é uma execução.
 * Os nomes das colunas são o contrato com o {@code ResultWriter} e ficam em português.
 */
public final class ResultReader {

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "esquema", "canal", "codigo", "ebn0_db", "semente", "bits_mensagem", "bits_errados");

    private ResultReader() {
    }

    /**
     * Uma execução, com só os campos que os gráficos usam. {@code scheme} é o nome do
     * código de linha ou da modulação (ex.: AMI, QAM16); {@code code} é NENHUM ou HAMMING74.
     */
    public record Run(String scheme, String channel, String code, double ebn0Db,
                      long seed, long messageBits, long bitErrors) {

        public Run {
            if (!Double.isFinite(ebn0Db)) {
                throw new IllegalArgumentException("Eb/N0 não finito: " + ebn0Db);
            }
            if (messageBits <= 0) {
                throw new IllegalArgumentException("bits_mensagem deve ser positivo: " + messageBits);
            }
            if (bitErrors < 0 || bitErrors > messageBits) {
                throw new IllegalArgumentException("bits_errados fora de [0, bits_mensagem]: "
                        + bitErrors + " de " + messageBits);
            }
        }
    }

    public static List<Run> read(Path csv) throws IOException {
        Objects.requireNonNull(csv, "csv não pode ser nulo");
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV vazio: " + csv);
        }
        // Remove o BOM que alguns editores e o PowerShell 5 colocam no início do arquivo
        String[] header = lines.get(0).replace("\uFEFF", "").split(",", -1);
        Map<String, Integer> columns = indexColumns(header, csv);

        List<Run> runs = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            int numeroDaLinha = i + 1;
            String[] fields = line.split(",", -1);
            if (fields.length != header.length) {
                throw new IllegalArgumentException("linha " + numeroDaLinha + " de " + csv
                        + " tem " + fields.length + " campos; o cabeçalho tem " + header.length);
            }
            runs.add(parseRun(fields, columns, numeroDaLinha, csv));
        }
        return runs;
    }

    private static Run parseRun(String[] fields, Map<String, Integer> columns,
                                int numeroDaLinha, Path csv) {
        try {
            return new Run(
                    field(fields, columns, "esquema"),
                    field(fields, columns, "canal"),
                    field(fields, columns, "codigo"),
                    Double.parseDouble(field(fields, columns, "ebn0_db")),
                    Long.parseLong(field(fields, columns, "semente")),
                    Long.parseLong(field(fields, columns, "bits_mensagem")),
                    Long.parseLong(field(fields, columns, "bits_errados")));
        } catch (IllegalArgumentException e) {
            // NumberFormatException também cai aqui, pois é subclasse de IllegalArgumentException
            throw new IllegalArgumentException("valor inválido na linha " + numeroDaLinha
                    + " de " + csv + ": " + e.getMessage(), e);
        }
    }

    private static String field(String[] fields, Map<String, Integer> columns, String name) {
        return fields[columns.get(name)].trim();
    }

    private static Map<String, Integer> indexColumns(String[] header, Path csv) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].trim(), i);
        }
        for (String name : REQUIRED_COLUMNS) {
            if (!columns.containsKey(name)) {
                throw new IllegalArgumentException("coluna ausente em " + csv + ": " + name);
            }
        }
        return columns;
    }
}