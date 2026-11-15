package br.unisinos.edu.codificacao.linha;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineCodeTest {

    @Test
    void eficienciaEspectralNominalDeAmiEManchester() {
        assertEquals(1.0, LineCode.AMI.eficienciaEspectral(), 1e-12);
        assertEquals(0.5, LineCode.MANCHESTER.eficienciaEspectral(), 1e-12);
    }

    @Test
    void manchesterUsaMaisBandaQueAmi() {
        // duas amostras por bit: o dobro da banda, metade da eficiência
        assertEquals(LineCode.AMI.eficienciaEspectral() / LineCode.MANCHESTER.amostrasPorBit(),
                LineCode.MANCHESTER.eficienciaEspectral(), 1e-12);
    }
}