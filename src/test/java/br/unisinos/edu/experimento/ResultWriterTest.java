package br.unisinos.edu.experimento;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Transmission;
import br.unisinos.edu.transmissao.TransmissionResult;
// Ajuste ao pacote real de DataGenerator.
import br.unisinos.edu.mensagem.DataGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultWriterTest {

    @Test
    void linhaTemOMesmoNumeroDeCamposDoCabecalho() {
        int[] bits = DataGenerator.bitsAleatorios(1000, 7);
        TransmissionResult r = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.AMI, true, ChannelType.AWGN, 6.0, 7));

        String[] campos = ResultWriter.linha(r).split(",", -1);
        assertEquals(ResultWriter.CABECALHO.split(",").length, campos.length);
        assertEquals("AMI", campos[0]);
        assertEquals("HAMMING74", campos[3]);
    }

    @Test
    void modulacaoUsaONomeDaModulacaoNaColunaEsquema() {
        int[] bits = DataGenerator.bitsAleatorios(1000, 7);
        TransmissionResult r = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.QAM16, false, ChannelType.AWGN, 6.0, 7));

        String[] campos = ResultWriter.linha(r).split(",", -1);
        assertEquals("QAM16", campos[0]);
        assertEquals("MODULADO", campos[1]);
        assertEquals("NENHUM", campos[3]);
    }
}