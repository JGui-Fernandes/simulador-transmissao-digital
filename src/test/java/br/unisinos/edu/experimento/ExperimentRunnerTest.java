package br.unisinos.edu.experimento;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Mode;
import br.unisinos.edu.transmissao.Transmission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentRunnerTest {

    /** 2 esquemas × 2 canais × 2 códigos × 2 Eb/N0 × 2 sementes = 32 execuções. */
    private static ExperimentoConfig pequeno() {
        return new ExperimentoConfig(
                List.of(0.0, 6.0), 10_000, List.of(1L, 2L),
                List.of(Modulation.BPSK), List.of(LineCode.AMI),
                List.of(ChannelType.AWGN, ChannelType.RAJADAS),
                List.of(false, true),
                0.01, 0.1, 5.0);
    }

    @Test
    void mesmasSementesGeramCsvIdentico(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.csv");
        Path b = tmp.resolve("b.csv");

        ExperimentRunner.executar(pequeno(), a, s -> { });
        ExperimentRunner.executar(pequeno(), b, s -> { });

        assertArrayEquals(Files.readAllBytes(a), Files.readAllBytes(b));
    }

    @Test
    void geraUmaLinhaPorExecucaoMaisCabecalho(@TempDir Path tmp) throws IOException {
        ExperimentoConfig cfg = pequeno();
        assertEquals(32, ExperimentRunner.total(cfg));

        Path saida = tmp.resolve("bruto.csv");
        ExperimentRunner.executar(cfg, saida, s -> { });

        List<String> linhas = Files.readAllLines(saida);
        assertEquals(ResultWriter.CABECALHO, linhas.get(0));
        assertEquals(33, linhas.size());
    }

    @Test
    void esquemasSaoUniaoSemCombinacoesInvalidas() {
        List<ExperimentRunner.Esquema> esquemas = ExperimentRunner.esquemas(pequeno());

        assertEquals(2, esquemas.size());
        assertEquals(Mode.MODULADO, esquemas.get(0).modo());
        assertEquals(Modulation.BPSK, esquemas.get(0).modulacao());
        assertNull(esquemas.get(0).linha());
        assertEquals(Mode.LINHA, esquemas.get(1).modo());
        assertEquals(LineCode.AMI, esquemas.get(1).linha());
        assertNull(esquemas.get(1).modulacao());
    }

    @Test
    void linhaDoRunnerEhIdenticaAChamadaDiretaATransmission(@TempDir Path tmp) throws IOException {
        ExperimentoConfig cfg = new ExperimentoConfig(
                List.of(4.0), 10_000, List.of(7L),
                List.of(Modulation.QPSK), List.of(),
                List.of(ChannelType.AWGN), List.of(false),
                0.01, 0.1, 5.0);

        Path saida = tmp.resolve("um.csv");
        ExperimentRunner.executar(cfg, saida, s -> { });
        String linhaDoRunner = Files.readAllLines(saida).get(1);

        int[] bits = DataGenerator.bitsAleatorios(10_000, 7L);
        ConfigTransmission config = ConfigTransmission
                .paraModulacao(Modulation.QPSK, false, ChannelType.AWGN, 4.0, 7L)
                .comRajadas(0.01, 0.1, 5.0);
        assertEquals(ResultWriter.linha(Transmission.executar(bits, config)), linhaDoRunner);
    }
}