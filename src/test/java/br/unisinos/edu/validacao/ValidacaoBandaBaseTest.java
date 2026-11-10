package br.unisinos.edu.validacao;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.codificacao.linha.LineDecoder;
import br.unisinos.edu.codificacao.linha.LineEncoder;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.metricas.Metrics;
import br.unisinos.edu.modulacao.Complex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validação da banda base contra as fórmulas teóricas de BER em AWGN.
 * Referências em Eb/N0 = 4 dB (10^0,4 ≈ 2,512):
 *   NRZ polar:  Q(sqrt(2·Eb/N0))       = Q(2,2414) ≈ 1,25·10⁻²
 *   Manchester: Q(sqrt(2·Eb/N0))       = mesmo valor do NRZ (Eb = 2 por bit)
 *   AMI:        1,5 · Q(sqrt(Eb/N0))   = 1,5 · Q(1,5849) ≈ 8,47·10⁻²
 */
class ValidacaoBandaBaseTest {

    private static final int N_BITS = 1_000_000;
    private static final long SEMENTE_DADOS = 2026L;
    private static final long SEMENTE_CANAL = 3026L;   // diferente da semente dos dados
    private static final double EBN0_TESTE_DB = 4.0;
    private static final double TOLERANCIA_RELATIVA = 0.05;

    @Test
    void nrzPolarEmQuatroDbSeAproximaDaTeoria() {
        int[] bits = DataGenerator.bitsAleatorios(N_BITS, SEMENTE_DADOS);

        double[] tx = nrzPolar(bits);
        double sigma = SnrConverter.sigmaBandaBase(EBN0_TESTE_DB, 1.0, 1.0);
        Complex[] rx = AwgnChannel.bandaBase()
                .apply(Complex.deReais(tx), sigma, new Random(SEMENTE_CANAL));

        int[] decididos = decidirPorSinal(Complex.partesReais(rx));
        double ber = Metrics.ber(bits, decididos).taxa();

        assertEquals(0.0125, ber, 0.0125 * TOLERANCIA_RELATIVA);
    }

    @Test
    void manchesterTemMesmaBerQueNrzPolarNoMesmoEbN0() {
        double ber = berSimulada(LineCode.MANCHESTER, EBN0_TESTE_DB);
        assertEquals(0.0125, ber, 0.0125 * TOLERANCIA_RELATIVA);
    }

    @Test
    void amiTemBerDaFormulaDe1Mais5QDeRaizDeEbN0() {
        double ber = berSimulada(LineCode.AMI, EBN0_TESTE_DB);
        assertEquals(0.0847, ber, 0.0847 * TOLERANCIA_RELATIVA);
    }

    @ParameterizedTest
    @EnumSource(LineCode.class)
    void semRuidoBerEhZero(LineCode codigo) {
        int[] bits = DataGenerator.bitsAleatorios(100_000, SEMENTE_DADOS);

        double[] tx = LineEncoder.codificar(bits, codigo);
        int[] recebidos = LineDecoder.decodificar(tx, codigo);

        assertEquals(0, Metrics.ber(bits, recebidos).bitsErrados());
    }

    @ParameterizedTest
    @EnumSource(LineCode.class)
    void emSnrMuitoAltoBerEhZeroComCanal(LineCode codigo) {
        // 40 dB: o ruído é dezenas de sigmas menor que a distância até a decisão
        assertEquals(0.0, berSimulada(codigo, 40.0), 0.0);
    }

    // ---------- auxiliares ----------

    /** Pipeline banda base completo: codificar, ruído AWGN, decodificar, medir BER. */
    private static double berSimulada(LineCode codigo, double ebn0Db) {
        int[] bits = DataGenerator.bitsAleatorios(N_BITS, SEMENTE_DADOS);

        double[] tx = LineEncoder.codificar(bits, codigo);
        double sigma = SnrConverter.sigmaBandaBase(
                ebn0Db, SnrConverter.energiaPorBitBandaBase(codigo), 1.0);
        Complex[] rx = AwgnChannel.bandaBase()
                .apply(Complex.deReais(tx), sigma, new Random(SEMENTE_CANAL));

        int[] decididos = LineDecoder.decodificar(Complex.partesReais(rx), codigo);
        return Metrics.ber(bits, decididos).taxa();
    }

    /** NRZ polar: bit 1 -> +1, bit 0 -> -1, uma amostra por bit. */
    private static double[] nrzPolar(int[] bits) {
        double[] amostras = new double[bits.length];
        for (int i = 0; i < bits.length; i++) {
            amostras[i] = bits[i] == 1 ? +1.0 : -1.0;
        }
        return amostras;
    }

    private static int[] decidirPorSinal(double[] amostras) {
        int[] bits = new int[amostras.length];
        for (int i = 0; i < amostras.length; i++) {
            bits[i] = amostras[i] > 0 ? 1 : 0;
        }
        return bits;
    }
}