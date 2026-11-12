package br.unisinos.edu.modulacao;

import br.unisinos.edu.mensagem.DataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class ModulatorDemodulatorTest {
    @Test
    void mapeamentoConhecido() {
        // BPSK: 1 -> +1, 0 -> -1
        Complex[] bpsk = Modulator.modular(new int[] {1, 0}, Modulation.BPSK);
        assertEquals(new Complex(+1, 0), bpsk[0]);
        assertEquals(new Complex(-1, 0), bpsk[1]);

        // 16-QAM: 1000 -> I = +3, Q = -3 (escala 1/√10)
        double e = 1.0 / Math.sqrt(10.0);
        Complex[] qam = Modulator.modular(new int[] {1, 0, 0, 0}, Modulation.QAM16);
        assertEquals(3 * e, qam[0].re(), 1e-12);
        assertEquals(-3 * e, qam[0].im(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void idaEVoltaParaTodosOsTamanhosDeBloco(Modulation m) {
        int k = m.bitsPorSimbolo();
        for (int n = 0; n <= 50; n++) { // inclui tamanhos que exigem padding
            int[] bits = DataGenerator.bitsAleatorios(n, 100L + n);
            Complex[] simbolos = Modulator.modular(bits, m);
            assertEquals((n + k - 1) / k, simbolos.length);
            assertArrayEquals(bits, Demodulator.demodular(simbolos, m, n),
                    m.rotulo() + ", n = " + n);
        }
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void decideCertoComPequenoDeslocamento(Modulation m) {
        // 0,1 em cada eixo é menor que a metade da menor distância (16-QAM: 1/√10 ≈ 0,316)
        int k = m.bitsPorSimbolo();
        for (int v = 0; v < m.ordem(); v++) {
            int[] bits = new int[k];
            for (int b = 0; b < k; b++) {
                bits[b] = (v >> (k - 1 - b)) & 1;
            }
            Complex original = Modulator.modular(bits, m)[0];
            Complex perturbado = original.soma(new Complex(0.1, -0.1));
            assertArrayEquals(bits,
                    Demodulator.demodular(new Complex[] {perturbado}, m, k),
                    m.rotulo() + ", símbolo " + v);
        }
    }

    @Test
    void rejeitaEntradasInvalidas() {
        assertThrows(IllegalArgumentException.class,
                () -> Modulator.modular(new int[] {0, 2}, Modulation.BPSK));
        assertThrows(IllegalArgumentException.class,
                () -> Demodulator.demodular(new Complex[1], Modulation.QPSK, 3)); // 3 bits exigem 2 símbolos
        assertThrows(NullPointerException.class,
                () -> Modulator.modular(null, Modulation.BPSK));
    }

    @Test
    void mapeamentoQpskSegueODiagrama() {
        // 1º bit -> I, 2º bit -> Q; 0 -> negativo, 1 -> positivo
        double e = 1.0 / Math.sqrt(2.0);
        assertEquals(new Complex(-e, -e), Modulator.modular(new int[] {0, 0}, Modulation.QPSK)[0]);
        assertEquals(new Complex(-e, +e), Modulator.modular(new int[] {0, 1}, Modulation.QPSK)[0]);
        assertEquals(new Complex(+e, -e), Modulator.modular(new int[] {1, 0}, Modulation.QPSK)[0]);
        assertEquals(new Complex(+e, +e), Modulator.modular(new int[] {1, 1}, Modulation.QPSK)[0]);
    }

    @Test
    void mapeamento16QamSegueODiagramaGray() {
        // Em cada eixo: 00 -> -3, 01 -> -1, 11 -> +1, 10 -> +3
        // Os 2 primeiros bits definem I e os 2 últimos definem Q.
        double e = 1.0 / Math.sqrt(10.0);
        for (int v = 0; v < 16; v++) {
            int[] bits = {(v >> 3) & 1, (v >> 2) & 1, (v >> 1) & 1, v & 1};
            Complex c = Modulator.modular(bits, Modulation.QAM16)[0];
            assertEquals(nivelGray(bits[0], bits[1]) * e, c.re(), 1e-12, "I do símbolo " + v);
            assertEquals(nivelGray(bits[2], bits[3]) * e, c.im(), 1e-12, "Q do símbolo " + v);
        }
    }

    private static int nivelGray(int a, int b) {
        if (a == 0 && b == 0) return -3;
        if (a == 0 && b == 1) return -1;
        if (a == 1 && b == 1) return +1;
        return +3; // 10
    }

    @Test
    void paddingEhInformadoECompletaOUltimoSimboloComZeros() {
        assertEquals(0, Modulator.bitsDePadding(8, Modulation.QAM16));
        assertEquals(3, Modulator.bitsDePadding(5, Modulation.QAM16));
        assertEquals(1, Modulator.bitsDePadding(5, Modulation.QPSK));
        assertEquals(0, Modulator.bitsDePadding(5, Modulation.BPSK));

        // 5 bits em 16-QAM: o 2º símbolo é "1" seguido de 3 zeros de padding, ou seja, "1000"
        Complex[] s = Modulator.modular(new int[] {0, 0, 0, 0, 1}, Modulation.QAM16);
        assertEquals(2, s.length);
        assertEquals(Modulator.modular(new int[] {1, 0, 0, 0}, Modulation.QAM16)[0], s[1]);
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void idaEVoltaComTextoDoTrabalho(Modulation m) {
        String texto = "Redes de Computadores - GB 2026!";
        int[] bits = DataGenerator.textoParaBits(texto);
        Complex[] simbolos = Modulator.modular(bits, m);
        assertEquals(texto, DataGenerator.bitsParaTexto(
                Demodulator.demodular(simbolos, m, bits.length)));
    }

    @Test
    void rejeitaTamanhosNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> Modulator.bitsDePadding(-1, Modulation.QAM16));
        assertThrows(IllegalArgumentException.class,
                () -> Demodulator.demodular(new Complex[0], Modulation.BPSK, -1));
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void energiaMediaDosSimbolosEhUm(Modulation m) {
        Complex[] constelacao = m.constelacao();

        double soma = 0.0;
        for (Complex c : constelacao) {
            soma += c.re() * c.re() + c.im() * c.im();
        }

        double energiaMedia = soma / constelacao.length;

        assertEquals(1.0, energiaMedia, 1e-12, m.rotulo());
    }
}
