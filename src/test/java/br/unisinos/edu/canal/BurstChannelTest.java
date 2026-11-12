package br.unisinos.edu.canal;

import br.unisinos.edu.modulacao.Complex;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BurstChannelTest {

    private static final double P = 0.01;
    private static final double R = 0.1;
    private static final double FATOR = 5.0;
    private static final int N = 1_000_000;

    private static Complex[] zeros(int n) {
        return Complex.deReais(new double[n]);
    }

    private static int contarRuins(boolean[] estados) {
        int c = 0;
        for (boolean e : estados) {
            if (e) c++;
        }
        return c;
    }

    // ---------- Estatística da cadeia de Markov ----------

    @Test
    void fracaoNoEstadoRuimFicaPertoDePSobrePMaisR() {
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        canal.apply(zeros(N), 1.0, new Random(3026L));

        double esperada = canal.fracaoEsperadaRuim(); // ≈ 0,0909
        double medida = (double) contarRuins(canal.estadosUltimaChamada()) / N;
        assertEquals(esperada, medida, 0.05 * esperada);
    }

    @Test
    void duracaoMediaDaRajadaFicaPertoDeUmSobreR() {
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        canal.apply(zeros(N), 1.0, new Random(3026L));
        boolean[] e = canal.estadosUltimaChamada();

        int rajadas = 0;
        for (int i = 0; i < e.length; i++) {
            if (e[i] && (i == 0 || !e[i - 1])) rajadas++;
        }
        double duracaoMedia = (double) contarRuins(e) / rajadas;
        assertEquals(canal.duracaoMediaRajada(), duracaoMedia, 0.05 * canal.duracaoMediaRajada());
    }

    @Test
    void varianciaDoRuidoEmCadaEstado() {
        double sigma = 0.4;
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        Complex[] saida = canal.apply(zeros(N), sigma, new Random(2026L));
        boolean[] e = canal.estadosUltimaChamada();

        double somaBom = 0, somaRuim = 0;
        int nBom = 0, nRuim = 0;
        for (int i = 0; i < N; i++) {
            double x = saida[i].re();
            if (e[i]) { somaRuim += x * x; nRuim++; }
            else      { somaBom  += x * x; nBom++; }
        }

        double varBom = sigma * sigma;
        double varRuim = (sigma * FATOR) * (sigma * FATOR);
        assertEquals(varBom, somaBom / nBom, 0.01 * varBom);
        assertEquals(varRuim, somaRuim / nRuim, 0.03 * varRuim);
    }

    // ---------- Reprodutibilidade e estado ----------

    @Test
    void mesmaSementeRepeteEstadosESaida() {
        BurstChannel a = BurstChannel.passaBanda(P, R, FATOR);
        BurstChannel b = BurstChannel.passaBanda(P, R, FATOR);

        Complex[] sa = a.apply(zeros(50_000), 0.5, new Random(42L));
        Complex[] sb = b.apply(zeros(50_000), 0.5, new Random(42L));

        assertArrayEquals(a.estadosUltimaChamada(), b.estadosUltimaChamada());
        assertArrayEquals(sa, sb);
    }

    @Test
    void sementesDiferentesGeramEstadosDiferentes() {
        BurstChannel a = BurstChannel.bandaBase(P, R, FATOR);
        BurstChannel b = BurstChannel.bandaBase(P, R, FATOR);
        a.apply(zeros(50_000), 0.5, new Random(1L));
        b.apply(zeros(50_000), 0.5, new Random(2L));

        assertFalse(Arrays.equals(a.estadosUltimaChamada(), b.estadosUltimaChamada()));
    }

    @Test
    void estadoEhMantidoEntreChamadas() {
        // Uma chamada de 20.000 amostras deve ser igual a duas de 10.000
        // com o mesmo Random, porque o estado continua de uma para a outra.
        BurstChannel inteiro = BurstChannel.bandaBase(P, R, FATOR);
        inteiro.apply(zeros(20_000), 0.5, new Random(7L));
        boolean[] esperado = inteiro.estadosUltimaChamada();

        BurstChannel partido = BurstChannel.bandaBase(P, R, FATOR);
        Random rnd = new Random(7L);
        partido.apply(zeros(10_000), 0.5, rnd);
        boolean[] primeira = partido.estadosUltimaChamada();
        partido.apply(zeros(10_000), 0.5, rnd);
        boolean[] segunda = partido.estadosUltimaChamada();

        boolean[] concatenado = new boolean[20_000];
        System.arraycopy(primeira, 0, concatenado, 0, 10_000);
        System.arraycopy(segunda, 0, concatenado, 10_000, 10_000);
        assertArrayEquals(esperado, concatenado);
    }

    @Test
    void reiniciarVoltaAoEstadoBomEReproduzAPrimeiraChamada() {
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        canal.apply(zeros(10_000), 0.5, new Random(1L));
        boolean[] primeira = canal.estadosUltimaChamada();

        canal.reiniciar();
        assertFalse(canal.estaNoEstadoRuim());

        canal.apply(zeros(10_000), 0.5, new Random(1L));
        assertArrayEquals(primeira, canal.estadosUltimaChamada());
    }

    // ---------- Comportamento básico ----------

    @Test
    void bandaBaseMantemParteImaginariaZero() {
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        Complex[] saida = canal.apply(zeros(10_000), 1.0, new Random(5L));
        for (Complex z : saida) {
            assertEquals(0.0, z.im());
        }
    }

    @Test
    void sigmaZeroNaoAlteraOSinal() {
        Complex[] entrada = Complex.deReais(new double[] {1.0, -1.0, 0.5});
        BurstChannel canal = BurstChannel.passaBanda(P, R, FATOR);
        assertArrayEquals(entrada, canal.apply(entrada, 0.0, new Random(5L)));
    }

    @Test
    void naoAlteraAEntrada() {
        Complex[] entrada = Complex.deReais(new double[] {1.0, -1.0, 1.0});
        Complex[] copia = entrada.clone();
        BurstChannel.passaBanda(P, R, FATOR).apply(entrada, 1.0, new Random(5L));
        assertArrayEquals(copia, entrada);
    }

    // ---------- Validação ----------

    @Test
    void rejeitaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> BurstChannel.bandaBase(0.0, R, FATOR));
        assertThrows(IllegalArgumentException.class, () -> BurstChannel.bandaBase(1.5, R, FATOR));
        assertThrows(IllegalArgumentException.class, () -> BurstChannel.bandaBase(P, Double.NaN, FATOR));
        assertThrows(IllegalArgumentException.class, () -> BurstChannel.bandaBase(P, R, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> BurstChannel.bandaBase(P, R, Double.POSITIVE_INFINITY));

        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        assertThrows(IllegalArgumentException.class, () -> canal.apply(zeros(1), -1.0, new Random()));
        assertThrows(IllegalArgumentException.class, () -> canal.apply(zeros(1), Double.NaN, new Random()));
        assertThrows(NullPointerException.class, () -> canal.apply(null, 1.0, new Random()));
        assertThrows(NullPointerException.class, () -> canal.apply(zeros(1), 1.0, null));
    }
}