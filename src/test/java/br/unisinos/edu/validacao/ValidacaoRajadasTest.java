package br.unisinos.edu.validacao;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.canal.BurstChannel;
import br.unisinos.edu.canal.Channel;
import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.modulacao.Complex;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cenário piloto do canal de rajadas: NRZ polar, Eb/N0 nominal de 4 dB,
 * p = 0,01, r = 0,1, fator = 5.
 */
class ValidacaoRajadasTest {

    private static final double EBN0_DB = 4.0;
    private static final int N = 1_000_000;
    private static final double P = 0.01;
    private static final double R = 0.1;
    private static final double FATOR = 5.0;

    /** Vetor com true nas posições de bit errado. */
    private static boolean[] erros(Channel canal) {
        int[] bits = DataGenerator.bitsAleatorios(N, 2026L);
        double[] nrz = new double[N];
        for (int i = 0; i < N; i++) {
            nrz[i] = bits[i] == 1 ? 1.0 : -1.0;
        }
        double sigma = SnrConverter.sigmaBandaBase(EBN0_DB, 1.0, 1.0);
        Complex[] rx = canal.apply(Complex.deReais(nrz), sigma, new Random(3026L));

        boolean[] e = new boolean[N];
        for (int i = 0; i < N; i++) {
            int decidido = rx[i].re() > 0 ? 1 : 0;
            e[i] = decidido != bits[i];
        }
        return e;
    }

    private static double ber(boolean[] e) {
        int c = 0;
        for (boolean x : e) if (x) c++;
        return (double) c / e.length;
    }

    /** Fração de pares vizinhos (i, i+1) com os dois bits errados. */
    private static double taxaErrosConsecutivos(boolean[] e) {
        int c = 0;
        for (int i = 0; i + 1 < e.length; i++) {
            if (e[i] && e[i + 1]) c++;
        }
        return (double) c / (e.length - 1);
    }

    @Test
    void cenarioPilotoDegradaBerSemInutilizarATransmissao() {
        double berAwgn = ber(erros(AwgnChannel.bandaBase()));
        double berRajada = ber(erros(BurstChannel.bandaBase(P, R, FATOR)));

        // Controle: o AWGN continua batendo com a teoria (≈ 0,0125).
        assertEquals(0.0125, berAwgn, 0.05 * 0.0125);

        // Degradação visível: esperado ≈ 0,041, cerca de 3,3 vezes o AWGN.
        assertTrue(berRajada > 2 * berAwgn,
                "rajadas: " + berRajada + ", AWGN: " + berAwgn);

        // Sem inutilizar: longe de 0,5 (chute aleatório).
        assertTrue(berRajada < 0.1, "BER com rajadas alta demais: " + berRajada);
    }

    @Test
    void errosDoCanalDeRajadasSaoAgrupados() {
        boolean[] e = erros(BurstChannel.bandaBase(P, R, FATOR));
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // Erros independentes com a mesma BER teriam P(dois seguidos) = BER².
        // Esperado: ≈ 0,0089 contra BER² ≈ 0,0017, cerca de 5 vezes.
        assertTrue(consecutivos > 2 * ber * ber,
                "P(consecutivos) = " + consecutivos + ", BER² = " + ber * ber);
    }

    @Test
    void errosDoAwgnSaoIndependentes() {
        // Controle: no AWGN, P(dois seguidos) ≈ BER².
        boolean[] e = erros(AwgnChannel.bandaBase());
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // ≈ 156 pares em 10⁶; o desvio de uma contagem de Poisson é ≈ 8%.
        assertEquals(ber * ber, consecutivos, 0.3 * ber * ber);
    }
}