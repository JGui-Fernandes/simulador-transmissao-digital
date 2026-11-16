package br.unisinos.edu.experimento;

import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Mode;
import br.unisinos.edu.transmissao.TransmissionResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Grava o CSV bruto, uma linha por execução. Não usa String.format nem o locale:
 * os números saem por Double.toString/Long.toString, para o arquivo ser idêntico
 * em qualquer máquina. Fim de linha sempre "\n".
 */
public final class ResultWriter implements AutoCloseable {

    public static final String CABECALHO = String.join(",",
            "esquema", "tipo", "canal", "codigo", "ebn0_db", "semente",
            "bits_mensagem", "bits_transmitidos", "bits_errados", "ber",
            "taxa_codigo", "eficiencia_bits_s_hz", "sigma", "p", "r", "fator");

    private final BufferedWriter saida;

    public ResultWriter(Path destino) throws IOException {
        Path pai = destino.toAbsolutePath().getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }
        this.saida = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
        saida.write(CABECALHO);
        saida.write('\n');
    }

    public void escrever(TransmissionResult resultado) throws IOException {
        saida.write(linha(resultado));
        saida.write('\n');
    }

    /** Linha CSV de um resultado, sem quebra de linha final. */
    public static String linha(TransmissionResult r) {
        ConfigTransmission c = r.config();
        boolean rajadas = c.canal() == ChannelType.RAJADAS;
        return String.join(",",
                nomeEsquema(c),
                c.modo().name(),
                c.canal().name(),
                c.hamming() ? "HAMMING74" : "NENHUM",
                Double.toString(c.ebn0Db()),
                Long.toString(c.semente()),
                Integer.toString(r.ber().totalBits()),
                Integer.toString(r.bitsTransmitidos()),
                Integer.toString(r.ber().bitsErrados()),
                Double.toString(r.ber().taxa()),
                Double.toString(r.taxaCodigo()),
                Double.toString(r.eficienciaEspectral()),
                Double.toString(r.sigma()),
                rajadas ? Double.toString(c.p()) : "",
                rajadas ? Double.toString(c.r()) : "",
                rajadas ? Double.toString(c.fatorDegradacao()) : "");
    }

    private static String nomeEsquema(ConfigTransmission c) {
        return c.modo() == Mode.LINHA ? c.linha().name() : c.modulacao().name();
    }

    @Override
    public void close() throws IOException {
        saida.close();
    }
}