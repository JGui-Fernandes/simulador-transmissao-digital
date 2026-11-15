package br.unisinos.edu.transmissao;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTransmissionTest {

    @Test
    void fabricaDeLinhaUsaOsParametrosPadraoDasRajadas() {
        ConfigTransmission c = ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, 4.0, 7L);
        assertEquals(Mode.LINHA, c.modo());
        assertEquals(LineCode.AMI, c.linha());
        assertNull(c.modulacao());
        assertEquals(ConfigTransmission.P_PADRAO, c.p());
        assertEquals(ConfigTransmission.R_PADRAO, c.r());
        assertEquals(ConfigTransmission.FATOR_PADRAO, c.fatorDegradacao());
    }

    @Test
    void fabricaModuladaGuardaModulacaoECodigoDeCanal() {
        ConfigTransmission c = ConfigTransmission.paraModulacao(Modulation.QAM16, true, ChannelType.RAJADAS, 8.0, 1L);
        assertEquals(Mode.MODULADO, c.modo());
        assertEquals(Modulation.QAM16, c.modulacao());
        assertNull(c.linha());
        assertTrue(c.hamming());
        assertEquals(ChannelType.RAJADAS, c.canal());
    }

    @Test
    void comRajadasTrocaSoOsParametrosDoCanal() {
        ConfigTransmission base = ConfigTransmission.paraModulacao(Modulation.QPSK, true, ChannelType.RAJADAS, 6.0, 9L);
        ConfigTransmission nova = base.comRajadas(0.02, 0.2, 4.0);
        assertEquals(0.02, nova.p());
        assertEquals(0.2, nova.r());
        assertEquals(4.0, nova.fatorDegradacao());
        assertEquals(base.modulacao(), nova.modulacao());
        assertEquals(base.ebn0Db(), nova.ebn0Db());
        assertEquals(base.semente(), nova.semente());
        assertEquals(base.hamming(), nova.hamming());
    }

    @Test
    void rejeitaModoSemOParametroCorrespondente() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigTransmission(
                Mode.LINHA, null, Modulation.BPSK, false, ChannelType.AWGN, 4.0, 1L, 0.01, 0.1, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new ConfigTransmission(
                Mode.MODULADO, LineCode.AMI, null, false, ChannelType.AWGN, 4.0, 1L, 0.01, 0.1, 5.0));
    }

    @Test
    void rejeitaParametrosInvalidos() {
        assertThrows(NullPointerException.class,
                () -> ConfigTransmission.paraLinha(LineCode.AMI, false, null, 4.0, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, Double.NaN, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, Double.POSITIVE_INFINITY, 1L));

        ConfigTransmission base = ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.RAJADAS, 4.0, 1L);
        assertThrows(IllegalArgumentException.class, () -> base.comRajadas(0.0, 0.1, 5.0));
        assertThrows(IllegalArgumentException.class, () -> base.comRajadas(0.01, 1.5, 5.0));
        assertThrows(IllegalArgumentException.class, () -> base.comRajadas(0.01, 0.1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> base.comRajadas(0.01, 0.1, Double.NaN));
    }
}