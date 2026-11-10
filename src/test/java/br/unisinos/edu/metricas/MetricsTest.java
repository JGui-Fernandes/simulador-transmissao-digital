package br.unisinos.edu.metricas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {

    @Test
    void semErrosRetornaBerZeroEListaVazia() {
        int[] bits = {1, 0, 1, 1};
        Metrics.ResultadoBer r = Metrics.ber(bits, bits.clone());

        assertEquals(0, r.bitsErrados());
        assertEquals(0.0, r.taxa(), 0.0);
        assertTrue(r.posicoesErradas().isEmpty());
    }

    @Test
    void identificaAsPosicoesDosBitsErrados() {
        int[] enviados = {1, 0, 1, 1};
        int[] recebidos = {1, 1, 1, 0};

        Metrics.ResultadoBer r = Metrics.ber(enviados, recebidos);

        assertEquals(4, r.totalBits());
        assertEquals(2, r.bitsErrados());
        assertEquals(List.of(1, 3), r.posicoesErradas());
        assertEquals(0.5, r.taxa(), 1e-12);
    }

    @Test
    void rejeitaTamanhosDiferentes() {
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.ber(new int[3], new int[4]));
    }

    @Test
    void rejeitaSequenciaVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.ber(new int[0], new int[0]));
    }
}