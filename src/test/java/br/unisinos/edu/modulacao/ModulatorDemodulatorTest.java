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

    private static double menorDistancia(Modulation m) {
        double d = Double.MAX_VALUE;
        for (int a = 0; a < m.ordem(); a++)
            for (int b = a + 1; b < m.ordem(); b++)
                d = Math.min(d, m.simbolo(a).distancia(m.simbolo(b)));
        return d;
    }

    private static int[] bitsDe(int valor, int k) {
        int[] bits = new int[k];
        for (int b = 0; b < k; b++) bits[b] = (valor >> (k - 1 - b)) & 1;
        return bits;
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void ruidoAbaixoDeDminSobreDoisNaoMudaADecisaoEmNenhumaDirecao(Modulation m) {
        int k = m.bitsPorSimbolo();
        double raio = 0.99 * menorDistancia(m) / 2.0; // d_min/2 = 1/√10 no 16-QAM
        for (int v = 0; v < m.ordem(); v++) {
            for (int d = 0; d < 8; d++) { // 0°, 45°, ..., 315°
                double ang = d * Math.PI / 4.0;
                Complex ruido = new Complex(raio * Math.cos(ang), raio * Math.sin(ang));
                assertArrayEquals(bitsDe(v, k),
                        Demodulator.demodular(new Complex[] {m.simbolo(v).soma(ruido)}, m, k),
                        m.rotulo() + ", símbolo " + v + ", direção " + 45 * d + "°");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void empurrarParaOVizinhoMaisProximoErraExatamenteUmBit(Modulation m) {
        int k = m.bitsPorSimbolo();
        double dMin = menorDistancia(m);
        for (int va = 0; va < m.ordem(); va++) {
            for (int vb = 0; vb < m.ordem(); vb++) {
                Complex a = m.simbolo(va), b = m.simbolo(vb);
                if (va == vb || Math.abs(a.distancia(b) - dMin) > 1e-9) continue;
                Complex empurrado = a.soma(b.soma(a.escala(-1.0)).escala(0.505)); // 1% além do ponto médio
                assertArrayEquals(bitsDe(vb, k),
                        Demodulator.demodular(new Complex[] {empurrado}, m, k),
                        m.rotulo() + ": " + va + " empurrado para " + vb);
                assertEquals(1, Integer.bitCount(va ^ vb));
            }
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

    private static void conferePonto(int[] bits, int i, int q) {
        double e = 1.0 / Math.sqrt(10.0);
        Complex c = Modulator.modular(bits, Modulation.QAM16)[0];
        assertEquals(i * e, c.re(), 1e-12);
        assertEquals(q * e, c.im(), 1e-12);
    }

    @Test
    void mapeamento16QamSegueATabelaDaEspecificacao() {
        // Em cada eixo: 00 -> -3, 01 -> -1, 11 -> +1, 10 -> +3 (valores escritos à mão)
        conferePonto(new int[] {0, 0, 0, 0}, -3, -3);
        conferePonto(new int[] {0, 1, 1, 0}, -1, +3);
        conferePonto(new int[] {1, 0, 1, 0}, +3, +3);
        conferePonto(new int[] {1, 1, 0, 0}, +1, -3);
        conferePonto(new int[] {0, 1, 0, 1}, -1, -1);
        conferePonto(new int[] {1, 1, 1, 1}, +1, +1);
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

    @Test
    void rejeitaSimboloRecebidoNaoFinito() {
        assertThrows(IllegalArgumentException.class, () -> Demodulator.demodular(
                new Complex[] {new Complex(Double.NaN, 0.0)}, Modulation.BPSK, 1));
        assertThrows(IllegalArgumentException.class, () -> Demodulator.demodular(
                new Complex[] {new Complex(0.0, Double.POSITIVE_INFINITY)}, Modulation.QPSK, 2));
    }
}
