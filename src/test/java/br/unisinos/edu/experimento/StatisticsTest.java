package br.unisinos.edu.experimento;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsTest {

    @Test
    void conjuntoPequenoBateComOCalculoAMao() {
        // média 3; variância amostral = (4+1+0+1+4)/4 = 2,5; dp = √2,5 ≈ 1,5811
        // IC95 = 3 ± 2,776 · 1,5811 / √5 ≈ 3 ± 1,96293
        Statistics.Resumo r = Statistics.resumir(new double[]{1, 2, 3, 4, 5});

        assertEquals(5, r.n());
        assertEquals(3.0, r.media(), 1e-12);
        assertEquals(Math.sqrt(2.5), r.desvioPadrao(), 1e-12);
        assertEquals(1.03707, r.ic95Inferior(), 1e-4);
        assertEquals(4.96293, r.ic95Superior(), 1e-4);
    }

    @Test
    void tParaQuatroGrausDeLiberdadeEh2Virgula776() {
        assertEquals(2.776, Statistics.tCritico95(4), 1e-12);
    }

    @Test
    void valoresConstantesDaoDesvioZeroEIntervaloDegenerado() {
        Statistics.Resumo r = Statistics.resumir(new double[]{0.02, 0.02, 0.02, 0.02, 0.02});
        assertEquals(0.0, r.desvioPadrao(), 1e-15);
        assertEquals(0.02, r.ic95Inferior(), 1e-15);
        assertEquals(0.02, r.ic95Superior(), 1e-15);
    }

    @Test
    void exigeAoMenosDuasRepeticoes() {
        assertThrows(IllegalArgumentException.class, () -> Statistics.resumir(new double[]{1.0}));
    }
}