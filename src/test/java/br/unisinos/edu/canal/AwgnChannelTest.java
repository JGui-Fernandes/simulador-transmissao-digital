package br.unisinos.edu.canal;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.modulacao.Complex;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AwgnChannelTest {

    private static final int N = 1_000_000;

    @Test
    void varianciaDoRuidoNaBandaBaseEhSigmaAoQuadrado() {
        double sigma = 0.5;
        Complex[] zeros = new Complex[N];
        Arrays.fill(zeros, Complex.ZERO);

        Complex[] saida = AwgnChannel.bandaBase().apply(zeros, sigma, new Random(1L));
        double[] re = Complex.partesReais(saida);

        assertEquals(sigma * sigma, variancia(re), sigma * sigma * 0.01);
        assertEquals(0.0, media(re), 0.005);
    }

    @Test
    void varianciaDaParteImaginariaNaPassaBandaEhSigmaAoQuadrado() {
        double sigma = 0.5;
        Complex[] zeros = new Complex[N];
        Arrays.fill(zeros, Complex.ZERO);

        Complex[] saida = AwgnChannel.passaBanda().apply(zeros, sigma, new Random(2L));

        assertEquals(sigma * sigma, variancia(imaginarias(saida)), sigma * sigma * 0.01);
    }

    @Test
    void bandaBaseNaoAdicionaRuidoNaParteImaginaria() {
        Complex[] sinal = Complex.deReais(new double[] {1.0, -1.0, 1.0, -1.0});
        Complex[] saida = AwgnChannel.bandaBase().apply(sinal, 0.8, new Random(3L));

        for (Complex z : saida) {
            assertEquals(0.0, z.im(), 0.0);
        }
    }

    @Test
    void sigmaZeroNaoAlteraOSinal() {
        double[] valores = {1.0, -1.0, 0.5};
        Complex[] sinal = Complex.deReais(valores);

        Complex[] saida = AwgnChannel.passaBanda().apply(sinal, 0.0, new Random(4L));

        assertArrayEquals(valores, Complex.partesReais(saida), 1e-12);
    }

    @Test
    void mesmaSementeGeraMesmoRuido() {
        Complex[] sinal = Complex.deReais(new double[1000]);

        Complex[] a = AwgnChannel.passaBanda().apply(sinal, 0.3, new Random(7L));
        Complex[] b = AwgnChannel.passaBanda().apply(sinal, 0.3, new Random(7L));

        assertArrayEquals(a, b);
    }

    @Test
    void naoAlteraOArrayDeEntrada() {
        Complex[] sinal = Complex.deReais(new double[] {1.0, -1.0});
        Complex[] copia = sinal.clone();

        AwgnChannel.bandaBase().apply(sinal, 0.5, new Random(5L));

        assertArrayEquals(copia, sinal);
    }

    @Test
    void rejeitaSigmaNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> AwgnChannel.bandaBase().apply(new Complex[1], -0.1, new Random(6L)));
    }

    // ---------- auxiliares ----------

    private static double media(double[] x) {
        return Arrays.stream(x).average().orElseThrow();
    }

    private static double variancia(double[] x) {
        double m = media(x);
        return Arrays.stream(x).map(v -> (v - m) * (v - m)).average().orElseThrow();
    }

    private static double[] imaginarias(Complex[] zs) {
        return Arrays.stream(zs).mapToDouble(Complex::im).toArray();
    }
}
