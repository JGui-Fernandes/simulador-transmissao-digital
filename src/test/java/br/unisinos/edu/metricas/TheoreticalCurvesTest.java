package br.unisinos.edu.metricas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TheoreticalCurvesTest {

    private static final double TOL_RELATIVA = 5e-4;

    private static void assertBerRelativa(String ponto, double esperado, double obtido) {
        assertEquals(esperado, obtido, Math.abs(esperado) * TOL_RELATIVA, ponto);
    }

    @Test
    void qEmZeroEhMeio() {
        assertEquals(0.5, TheoreticalCurves.q(0.0), 1e-12);
    }

    @Test
    void qTemValoresConhecidos() {
        assertEquals(0.1586552539, TheoreticalCurves.q(1.0), 1e-9);
        assertEquals(0.0227501320, TheoreticalCurves.q(2.0), 1e-9);
    }

    @Test
    void qEhSimetrica() {
        assertEquals(
                1.0,
                TheoreticalCurves.q(1.3) + TheoreticalCurves.q(-1.3),
                1e-12
        );
    }

    @Test
    void qNaCaudaEhPositivoEMuitoPequeno() {
        // Q(10) ≈ 7,6·10⁻²⁴: positivo, pequeno, nem zero nem NaN
        double q10 = TheoreticalCurves.q(10.0);
        assertTrue(q10 > 0.0 && q10 < 1e-20, "Q(10) = " + q10);
    }

    @Test
    void bpskBateComValoresTeoricos() {
        assertBerRelativa("0 dB", 0.0786496, TheoreticalCurves.berBpskQpsk(0.0));
        assertBerRelativa("4 dB", 0.0125008, TheoreticalCurves.berBpskQpsk(4.0));
        assertBerRelativa("8 dB", 0.0001909, TheoreticalCurves.berBpskQpsk(8.0));
    }

    @Test
    void qam16BateComValoresDaAproximacao() {
        assertBerRelativa("0 dB", 0.1391600, TheoreticalCurves.ber16Qam(0.0));
        assertBerRelativa("4 dB", 0.0586185, TheoreticalCurves.ber16Qam(4.0));
        assertBerRelativa("8 dB", 0.0092472, TheoreticalCurves.ber16Qam(8.0));
    }

    @Test
    void qam16ExigeMaisEbN0DoQueBpsk() {
        assertTrue(
                TheoreticalCurves.ber16Qam(4.0) > TheoreticalCurves.berBpskQpsk(4.0)
        );
    }

    @Test
    void berDiminuiQuandoEbN0Aumenta() {
        for (double db = -2.0; db < 12.0; db += 0.5) {
            assertTrue(
                    TheoreticalCurves.berBpskQpsk(db + 0.5) < TheoreticalCurves.berBpskQpsk(db),
                    "BPSK/QPSK em " + db + " dB"
            );
            assertTrue(
                    TheoreticalCurves.ber16Qam(db + 0.5) < TheoreticalCurves.ber16Qam(db),
                    "16-QAM em " + db + " dB"
            );
        }
    }

    @Test
    void berFicaNoIntervaloValido() {
        for (double db = -2.0; db <= 12.0; db += 0.5) {
            double bpsk = TheoreticalCurves.berBpskQpsk(db);
            double qam = TheoreticalCurves.ber16Qam(db);
            assertTrue(bpsk > 0.0 && bpsk < 0.5, "BPSK em " + db + " dB: " + bpsk);
            assertTrue(qam > 0.0 && qam < 0.5, "16-QAM em " + db + " dB: " + qam);
        }
    }
}