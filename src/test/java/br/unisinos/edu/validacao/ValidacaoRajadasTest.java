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
 *
 * O sigma é o nominal do AWGN no mesmo Eb/N0: o estado bom usa esse sigma e o
 * estado ruim usa sigma * fator. A potência média do ruído nas rajadas é, portanto,
 * maior que a do AWGN, e o Eb/N0 do eixo é o nominal, não o efetivo.
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

    /** Erros do canal de rajadas com o sigma nominal do AWGN. */
    private static boolean[] errosRajadas() {
        return erros(BurstChannel.bandaBase(P, R, FATOR), SIGMA_AWGN);
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
    void comSigmaNominalRajadasTemBerMaiorQueAwgn() {
        double berAwgn = ber(erros(AwgnChannel.bandaBase(), SIGMA_AWGN));
        double berRajadas = ber(errosRajadas());

        // Controle: o AWGN bate com a teoria, Q(√(2·10^0,4)) ≈ 0,0125.
        assertEquals(0.0125, berAwgn, 0.05 * 0.0125);

        // Estado bom: BER ≈ 0,0126. Estado ruim: σ·5 dá BER ≈ 0,327.
        // Média: 0,909·0,0126 + 0,091·0,327 ≈ 0,041, cerca de 3,3 vezes o AWGN.
        assertTrue(berRajadas > 2 * berAwgn,
                "rajadas: " + berRajadas + ", AWGN: " + berAwgn);
        assertTrue(berRajadas < 0.1, "BER com rajadas alta demais: " + berRajadas);
    }

    @Test
    void comFatorUnitarioSeComportaComoAwgn() {
        // Sem degradação, o canal de rajadas deve dar a mesma BER do AWGN
        // (dentro da variação amostral, com 10⁶ bits).
        double berAwgn = ber(erros(AwgnChannel.bandaBase(), SIGMA_AWGN));
        double berSemDegradacao = ber(erros(BurstChannel.bandaBase(P, R, 1.0), SIGMA_AWGN));

        assertEquals(berAwgn, berSemDegradacao, 0.05 * berAwgn);
    }

    @Test
    void errosDoCanalDeRajadasSaoAgrupados() {
        boolean[] e = errosRajadas();
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // Erros independentes com a mesma BER: P(dois seguidos) = BER².
        assertTrue(consecutivos > 3 * ber * ber,
                "P(consecutivos) = " + consecutivos + ", BER² = " + ber * ber);
    }

    @Test
    void errosDoAwgnSaoIndependentes() {
        // Controle: no AWGN, P(dois seguidos) ≈ BER².
        boolean[] e = erros(AwgnChannel.bandaBase(), SIGMA_AWGN);
        double ber = ber(e);
        double consecutivos = taxaErrosConsecutivos(e);

        // ≈ 160 pares em 10⁶; o desvio de uma contagem de Poisson é ≈ 8%.
        assertEquals(ber * ber, consecutivos, 0.3 * ber * ber);
    }
}