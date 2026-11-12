package br.unisinos.edu.codificacao.canal;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.codificacao.linha.LineDecoder;
import br.unisinos.edu.codificacao.linha.LineEncoder;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.metricas.Metrics;
import br.unisinos.edu.modulacao.Complex;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class HammingPipelineTest {
    private static final LineCode CODIGO = LineCode.MANCHESTER;

    private static int[] transmitirComSigma(int[] bits, boolean comHamming, double sigma, long semente) {
        Hamming74 h = new Hamming74();
        int[] tx = comHamming ? h.encode(bits) : bits;
        Complex[] rx = AwgnChannel.bandaBase().apply(
                Complex.deReais(LineEncoder.codificar(tx, CODIGO)), sigma, new Random(semente));
        int[] detectados = LineDecoder.decodificar(Complex.partesReais(rx), CODIGO);
        return comHamming ? h.decode(detectados, bits.length) : detectados;
    }

    private static int[] transmitir(int[] bits, boolean comHamming, double ebn0Db, long semente) {
        double taxa = comHamming ? new Hamming74().taxa() : 1.0;
        double sigma = SnrConverter.sigmaBandaBase(
                ebn0Db, SnrConverter.energiaPorBitBandaBase(CODIGO), taxa);
        return transmitirComSigma(bits, comHamming, sigma, semente);
    }

    @Test
    void pipelineComHammingSemRuidoRecuperaTexto() {
        String texto = "Redes de Computadores - GB 2026!";
        int[] bits = DataGenerator.textoParaBits(texto);
        int[] rec = transmitirComSigma(bits, true, 0.0, 1L);
        assertEquals(texto, DataGenerator.bitsParaTexto(rec));
    }


    @Test
    void emEbN0AltoBerComHammingEhMenorQueSemCodigo() {
        int[] bits = DataGenerator.bitsAleatorios(1_000_000, 11L);
        double berSem = Metrics.ber(bits, transmitir(bits, false, 8.0, 21L)).taxa();
        double berCom = Metrics.ber(bits, transmitir(bits, true, 8.0, 21L)).taxa();
        assertTrue(berCom < berSem, "com Hamming: " + berCom + " | sem código: " + berSem);
    }
}
