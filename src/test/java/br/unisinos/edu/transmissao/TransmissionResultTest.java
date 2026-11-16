package br.unisinos.edu.transmissao;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.metricas.Metrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransmissionResultTest {

    private static TransmissionResult resultado(int[] enviados, int[] recebidos) {
        return new TransmissionResult(
                ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, 10.0, 1),
                enviados, recebidos, Metrics.ber(enviados, recebidos),
                enviados.length, 1.0, 1.0, 0.1);
    }

    @Test
    void alterarOArrayDeOrigemNaoAlteraORessultado() {
        int[] enviados = {1, 0, 1, 1};
        TransmissionResult r = resultado(enviados, new int[]{1, 0, 1, 1});

        enviados[0] = 0;

        assertEquals(1, r.bitsEnviados()[0]);
    }

    @Test
    void alterarOArrayDevolvidoNaoAlteraOResultado() {
        TransmissionResult r = resultado(new int[]{1, 0, 1, 1}, new int[]{1, 0, 1, 1});

        r.bitsRecebidos()[0] = 0;

        assertEquals(1, r.bitsRecebidos()[0]);
        assertTrue(r.mensagemRecuperada());
    }
}