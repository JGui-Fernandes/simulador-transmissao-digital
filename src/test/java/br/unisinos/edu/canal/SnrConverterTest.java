package br.unisinos.edu.canal;

import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.codificacao.canal.Hamming74;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnrConverterTest {

    @Test
    void zeroDbComK2ERUmDaEsn0De3DbComoNoEnunciado() {
        assertEquals(3.0103, SnrConverter.ebn0ParaEsn0Db(0.0, 2, 1.0), 1e-4);
    }

    @Test
    void conversaoNosDoisSentidosEhIdentidade() {
        int[] ks = {1, 2, 4};
        double[] taxas = {1.0, 4.0 / 7.0};
        for (double ebn0 = -2; ebn0 <= 12; ebn0 += 2) {
            for (int k : ks) {
                for (double r : taxas) {
                    double esn0 = SnrConverter.ebn0ParaEsn0Db(ebn0, k, r);
                    assertEquals(ebn0, SnrConverter.esn0ParaEbn0Db(esn0, k, r), 1e-9);
                }
            }
        }
    }

    @Test
    void codigoHamming74AumentaEsn0EmCercaDe3Db() {
        // k = 4, R = 4/7: 10·log10(16/7) ≈ 3,59 dB
        assertEquals(3.59, SnrConverter.ebn0ParaEsn0Db(0.0, 4, 4.0 / 7.0), 0.01);
    }

    @Test
    void esn0DeZeroDbResultaEmSigmaRaizDeMeio() {
        // σ² = 1 / (2 · 1) = 0,5
        assertEquals(Math.sqrt(0.5), SnrConverter.sigmaPassaBanda(0.0), 1e-12);
    }

    @Test
    void bpskPassaBandaEBandaBaseNrzTemMesmoSigmaNoMesmoEbN0() {
        // Com k = 1 e R = 1, Es/N0 = Eb/N0 e os dois caminhos devem concordar
        double esn0 = SnrConverter.ebn0ParaEsn0Db(4.0, 1, 1.0);
        assertEquals(SnrConverter.sigmaPassaBanda(esn0),
                SnrConverter.sigmaBandaBase(4.0, 1.0, 1.0), 1e-12);
    }

    @Test
    void energiaPorBitDaBandaBase() {
        assertEquals(0.5, SnrConverter.energiaPorBitBandaBase(LineCode.AMI), 1e-12);
        assertEquals(2.0, SnrConverter.energiaPorBitBandaBase(LineCode.MANCHESTER), 1e-12);
    }

    @Test
    void rejeitaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> SnrConverter.ebn0ParaEsn0Db(0.0, 0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> SnrConverter.ebn0ParaEsn0Db(0.0, 2, 1.5));
        assertThrows(IllegalArgumentException.class,
                () -> SnrConverter.sigmaBandaBase(0.0, 0.0, 1.0));
    }

    @Test
    void taxaDoHammingAumentaSigmaPorRaizDe7Sobre4() {

        double eb = SnrConverter.energiaPorBitBandaBase(LineCode.MANCHESTER);
        double semCodigo = SnrConverter.sigmaBandaBase(6.0, eb, 1.0);
        double comCodigo = SnrConverter.sigmaBandaBase(6.0, eb, new Hamming74().taxa());
        assertEquals(Math.sqrt(7.0 / 4.0), comCodigo / semCodigo, 1e-12);

        assertEquals(-2.43, SnrConverter.ebn0ParaEsn0Db(0.0, 1, new Hamming74().taxa()), 0.01);
    }
}