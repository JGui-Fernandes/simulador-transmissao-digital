package br.unisinos.edu.validacao;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.metricas.Metrics;
import br.unisinos.edu.metricas.TheoreticalCurves;
import br.unisinos.edu.modulacao.Complex;
import br.unisinos.edu.modulacao.Demodulator;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.modulacao.Modulator;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ValidacaoTeoricaTest {

    private static final long SEMENTE_DADOS = 2026L;
    private static final long SEMENTE_CANAL = 3026L;
    private static final int BITS_POR_BLOCO = 1_000_000;
    private static final double TOLERANCIA = 0.15;

    /**
     * BER medida somando blocos independentes. Cada bloco usa sementes
     * diferentes para dados e para canal (dados = base + b, canal = base + b).
     */
    private static double berSimulada(Modulation m, double ebn0Db, int blocos) {
        // Sem codificação de canal, taxa R = 1.0
        double esn0Db = SnrConverter.ebn0ParaEsn0Db(ebn0Db, m.bitsPorSimbolo(), 1.0);
        double sigma = SnrConverter.sigmaPassaBanda(esn0Db);

        long erros = 0;
        long total = 0;
        for (int b = 0; b < blocos; b++) {
            int[] bits = DataGenerator.bitsAleatorios(BITS_POR_BLOCO, SEMENTE_DADOS + b);
            Complex[] simbolos = Modulator.modular(bits, m);
            Complex[] recebidos = AwgnChannel.passaBanda()
                    .apply(simbolos, sigma, new Random(SEMENTE_CANAL + b));
            int[] decididos = Demodulator.demodular(recebidos, m, BITS_POR_BLOCO);

            Metrics.ResultadoBer r = Metrics.ber(bits, decididos);
            erros += r.bitsErrados();
            total += r.totalBits();
        }
        return (double) erros / total;
    }

    private static void assertBerProxima(String rotulo, double ebn0Db,
                                         double teorica, double simulada) {
        double desvio = Math.abs(simulada - teorica) / teorica;
        assertTrue(desvio <= TOLERANCIA, String.format(
                "%s em %.0f dB: teórica=%.4e, simulada=%.4e, desvio=%.1f%% (limite %.0f%%)",
                rotulo, ebn0Db, teorica, simulada, desvio * 100, TOLERANCIA * 100));
    }

    @Test
    void bpskEm0E4dBBateComATeoria() {
        for (double ebn0 : new double[] {0.0, 4.0}) {
            assertBerProxima("BPSK", ebn0,
                    TheoreticalCurves.berBpskQpsk(ebn0),
                    berSimulada(Modulation.BPSK, ebn0, 1));
        }
    }

    @Test
    void bpskEm8dBComDezMilhoesDeBits() {
        assertBerProxima("BPSK", 8.0,
                TheoreticalCurves.berBpskQpsk(8.0),
                berSimulada(Modulation.BPSK, 8.0, 10));
    }

    @Test
    void qpskEm0E4dBBateComATeoriaDoBpsk() {
        // QPSK tem a mesma BER por bit que BPSK no mesmo Eb/N0
        for (double ebn0 : new double[] {0.0, 4.0}) {
            assertBerProxima("QPSK", ebn0,
                    TheoreticalCurves.berBpskQpsk(ebn0),
                    berSimulada(Modulation.QPSK, ebn0, 1));
        }
    }

    @Test
    void qpskEm8dBComDezMilhoesDeBits() {
        assertBerProxima("QPSK", 8.0,
                TheoreticalCurves.berBpskQpsk(8.0),
                berSimulada(Modulation.QPSK, 8.0, 10));
    }

    @Test
    void criterioDetectaDesvioDe3dBNoSnr() {
        // Garante que a tolerância não é folgada demais: um SNR 3 dB errado
        // deve ser reprovado pelo mesmo critério.
        double simulada = berSimulada(Modulation.BPSK, 4.0, 1);
        double teoricaComSnrErrado = TheoreticalCurves.berBpskQpsk(1.0);
        double desvio = Math.abs(simulada - teoricaComSnrErrado) / teoricaComSnrErrado;
        assertTrue(desvio > TOLERANCIA,
                "Tolerância grande demais: desvio de 3 dB passou com " + desvio);
    }
}