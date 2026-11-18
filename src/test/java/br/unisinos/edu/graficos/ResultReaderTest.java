package br.unisinos.edu.graficos;

import br.unisinos.edu.experimento.ResultWriter;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Transmission;
import br.unisinos.edu.transmissao.TransmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultReaderTest {

    private static final String CABECALHO_MINIMO =
            "esquema,canal,codigo,ebn0_db,semente,bits_mensagem,bits_errados\n";

    @TempDir
    Path tmp;

    @Test
    void leColunasPeloNomeIndependenteDaOrdemEIgnoraBom() throws IOException {
        Path csv = tmp.resolve("r.csv");
        Files.writeString(csv,
                "\uFEFFbits_errados,semente,esquema,tipo,canal,codigo,ebn0_db,bits_mensagem,ber,sigma,p,r,fator\n"
                        + "3,7,BPSK,MODULADO,AWGN,NENHUM,4.0,1000,0.003,0.7,,,\n",
                StandardCharsets.UTF_8);

        List<ResultReader.Run> runs = ResultReader.read(csv);

        assertEquals(1, runs.size());
        ResultReader.Run run = runs.get(0);
        assertEquals("BPSK", run.scheme());
        assertEquals("AWGN", run.channel());
        assertEquals("NENHUM", run.code());
        assertEquals(4.0, run.ebn0Db(), 0.0);
        assertEquals(7, run.seed());
        assertEquals(1000, run.messageBits());
        assertEquals(3, run.bitErrors());
    }

    @Test
    void lerOCsvGravadoPeloResultWriter() throws IOException {
        // Teste de contrato: o ResultWriter e o ResultReader precisam concordar nos nomes das colunas
        int[] bits = DataGenerator.bitsAleatorios(1_000, 5L);
        TransmissionResult resultado = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.QPSK, false, ChannelType.AWGN, 4.0, 5L));
        Path csv = tmp.resolve("ida_e_volta.csv");
        try (ResultWriter escritor = new ResultWriter(csv)) {
            escritor.escrever(resultado);
        }

        ResultReader.Run run = ResultReader.read(csv).get(0);

        assertEquals("QPSK", run.scheme());
        assertEquals("AWGN", run.channel());
        assertEquals("NENHUM", run.code());
        assertEquals(4.0, run.ebn0Db(), 0.0);
        assertEquals(5, run.seed());
        assertEquals(1_000, run.messageBits());
        assertEquals(resultado.ber().bitsErrados(), run.bitErrors());
    }

    @Test
    void colunaAusenteFalhaNomeandoAColuna() throws IOException {
        Path csv = tmp.resolve("ruim.csv");
        Files.writeString(csv, "esquema,tipo\nBPSK,MODULADO\n", StandardCharsets.UTF_8);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResultReader.read(csv));
        assertTrue(e.getMessage().contains("canal"), e.getMessage());
    }

    @Test
    void linhaComNumeroDeCamposErradoInformaONumeroDaLinha() throws IOException {
        Path csv = tmp.resolve("campos.csv");
        Files.writeString(csv, CABECALHO_MINIMO + "BPSK,AWGN,NENHUM,4.0,1\n", StandardCharsets.UTF_8);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResultReader.read(csv));
        assertTrue(e.getMessage().contains("linha 2"), e.getMessage());
    }

    @Test
    void errosMaioresQueOTotalDeBitsSaoRejeitados() throws IOException {
        Path csv = tmp.resolve("contagem.csv");
        Files.writeString(csv, CABECALHO_MINIMO + "BPSK,AWGN,NENHUM,4.0,1,1000,2000\n",
                StandardCharsets.UTF_8);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResultReader.read(csv));
        assertTrue(e.getMessage().contains("linha 2"), e.getMessage());
    }

    @Test
    void ignoraLinhasEmBranco() throws IOException {
        Path csv = tmp.resolve("branco.csv");
        Files.writeString(csv, CABECALHO_MINIMO
                + "BPSK,AWGN,NENHUM,4.0,1,1000,3\n"
                + "\n"
                + "BPSK,AWGN,NENHUM,4.0,2,1000,5\n", StandardCharsets.UTF_8);

        assertEquals(2, ResultReader.read(csv).size());
    }
}