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
 * Cenário piloto do canal de rajadas: NRZ polar, Eb/N0 de 4 dB,
 * p = 0,01, r = 0,1, fator = 5.
 *
 * Comparação principal no MESMO Eb/N0 médio: o sigma do estado bom é reduzido
 * para que a potência média do ruído seja igual à do AWGN. O teste nominal
 * mostra o efeito de somar ruído extra.
 */
class ValidacaoRajadasTest {

    private static final double EBN0_DB = 4.0;
    private static final int N = 1_000_000;
    private static final double P = 0.01;
    private static final double R = 0.1;
    private static final double FATOR = 5.0;
    private static final double SIGMA_AWGN = SnrConverter.sigmaBandaBase(EBN0_DB, 1.0, 1.0);

    /** Vetor com true nas posições de bit errado. */
    private static boolean[] erros(Channel canal, double sigma) {
        int[] bits = DataGenerator.bitsAleatorios(N, 2026L);
        double[] nrz = new double[N];
        for (int i = 0; i < N; i++) {
            nrz[i] = bits[i] == 1 ? 1.0 : -1.0;
        }
        Complex[] rx = canal.apply(Complex.deReais(nrz), sigma, new Random(3026L));

        boolean[] e = new boolean[N];
        for (int i = 0; i < N; i++) {
            int decidido = rx[i].re() > 0 ? 1 : 0;
            e[i] = decidido != bits[i];
        }
        return e;
    }

    /** Erros do canal de rajadas no mesmo Eb/N0 médio do AWGN. */
    private static boolean[] errosRajadasNormalizado() {
        BurstChannel canal = BurstChannel.bandaBase(P, R, FATOR);
        return erros(canal, canal.sigmaBomParaMesmaPotenciaMedia(SIGMA_AWGN));
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
    void noMesmoEbN0MedioRajadasTemBerMaiorQueAwgn() {
        double berAwgn = ber(erros(AwgnChannel.bandaBase(), SIGMA_AWGN));
        double berRajada = ber(errosRajadasNormalizado());

        // Controle: o AWGN bate com a teoria, Q(√(2·10^0,4)) ≈ 0,0125.
        assertEquals(0.0125, berAwgn, 0.05 * 0.0125);

        // Mesma potência média de ruído, distribuída em rajadas:
        // esperado ≈ 0,019, cerca de 1,5 vez o AWGN.
        assertTrue(berRajada > 1.3 * berAwgn,
                "rajadas: " + berRajada + ", AWGN: " + berAwgn);
        assertTrue(berRajada < 0.1, "BER com rajadas alta demais: " + berRajada);
    }

    @Test
    void errosDoCanalDeRajadasSaoAgrupados() {
        boolean[] e = errosRajadasNormalizado();
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // Erros independentes com a mesma BER: P(dois seguidos) = BER².
        // Esperado: ≈ 0,0037 contra BER² ≈ 0,00037, cerca de 10 vezes.
        assertTrue(consecutivos > 3 * ber * ber,
                "P(consecutivos) = " + consecutivos + ", BER² = " + ber * ber);
    }

    @Test
    void errosDoAwgnSaoIndependentes() {
        // Controle: no AWGN, P(dois seguidos) ≈ BER².
        boolean[] e = erros(AwgnChannel.bandaBase(), SIGMA_AWGN);
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // ≈ 156 pares em 10⁶; o desvio de uma contagem de Poisson é ≈ 8%.
        assertEquals(ber * ber, consecutivos, 0.3 * ber * ber);
    }

    @Test
    void comSigmaNominalADegradacaoEhMaiorPorqueHaMaisRuido() {
        // Mesmo sigma no estado bom, sem normalizar: a potência média do ruído
        // fica cerca de 3,18 vezes a do AWGN (≈ +5 dB). Esperado ≈ 0,041.
        double berAwgn = ber(erros(AwgnChannel.bandaBase(), SIGMA_AWGN));
        double berNominal = ber(erros(BurstChannel.bandaBase(P, R, FATOR), SIGMA_AWGN));
        double berNormalizada = ber(errosRajadasNormalizado());

        assertTrue(berNominal > berNormalizada,
                "nominal: " + berNominal + ", normalizada: " + berNormalizada);
        assertTrue(berNominal > 2 * berAwgn);
    }
}