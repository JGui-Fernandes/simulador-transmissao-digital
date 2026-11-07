package br.unisinos.edu.codificacao.linha;

import br.unisinos.edu.mensagem.DataGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineEncoderTest {

    // ---------- AMI ----------

    @Test
    void amiAlternaPolaridadeAFazCadaUm() {
        double[] s = LineEncoder.codificar(new int[] {1, 1, 1, 1}, LineCode.AMI);
        assertArrayEquals(new double[] {+1, -1, +1, -1}, s, 1e-12);
    }

    @Test
    void amiZerosPermanecemZero() {
        double[] s = LineEncoder.codificar(new int[] {0, 0, 0}, LineCode.AMI);
        assertArrayEquals(new double[] {0, 0, 0}, s, 1e-12);
    }

    @Test
    void amiZeroNaoAlteraAPolaridadeDoProximoUm() {
        // 1 0 1 1 0 1  ->  +1 0 -1 +1 0 -1
        double[] s = LineEncoder.codificar(new int[] {1, 0, 1, 1, 0, 1}, LineCode.AMI);
        assertArrayEquals(new double[] {+1, 0, -1, +1, 0, -1}, s, 1e-12);
    }

    @Test
    void amiNuncaRepetePolaridadeEntrePulsosConsecutivos() {
        int[] bits = DataGenerator.bitsAleatorios(10_000, 1L);
        double[] s = LineEncoder.codificar(bits, LineCode.AMI);

        double ultimoPulso = 0.0;   // polaridade do último pulso não nulo
        for (double x : s) {
            if (x != 0.0) {
                if (ultimoPulso != 0.0) {
                    assertEquals(-ultimoPulso, x, 1e-12,
                            "Dois pulsos consecutivos com a mesma polaridade");
                }
                ultimoPulso = x;
            }
        }
    }

    // ---------- Manchester ----------

    @Test
    void manchesterUsaAConvencaoDocumentada() {
        // 1 -> (+1, -1);  0 -> (-1, +1)
        double[] s = LineEncoder.codificar(new int[] {1, 0}, LineCode.MANCHESTER);
        assertArrayEquals(new double[] {+1, -1, -1, +1}, s, 1e-12);
    }

    @Test
    void manchesterGeraDuasAmostrasPorBit() {
        int[] bits = DataGenerator.bitsAleatorios(500, 2L);
        assertEquals(2 * bits.length,
                LineEncoder.codificar(bits, LineCode.MANCHESTER).length);
    }

    @Test
    void manchesterTemTransicaoNoMeioDeCadaBit() {
        int[] bits = DataGenerator.bitsAleatorios(10_000, 3L);
        double[] s = LineEncoder.codificar(bits, LineCode.MANCHESTER);

        for (int i = 0; i < bits.length; i++) {
            double primeiraMetade = s[2 * i];
            double segundaMetade = s[2 * i + 1];
            // as duas metades têm sinais opostos: sempre há transição no meio do bit
            assertEquals(-primeiraMetade, segundaMetade, 1e-12,
                    "Bit " + i + " sem transição no meio");
            // o sinal da primeira metade identifica o bit
            assertEquals(bits[i] == 1 ? +1.0 : -1.0, primeiraMetade, 1e-12,
                    "Bit " + i + " com polaridade errada");
        }
    }

    // ---------- Validação de entrada ----------

    @Test
    void rejeitaBitsDiferentesDeZeroEUm() {
        assertThrows(IllegalArgumentException.class,
                () -> LineEncoder.codificar(new int[] {0, 2}, LineCode.AMI));
    }

    @Test
    void rejeitaEntradaNula() {
        assertThrows(NullPointerException.class,
                () -> LineEncoder.codificar(null, LineCode.AMI));
    }
}
