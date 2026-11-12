package br.unisinos.edu.modulacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Arrays;

class ModulationTest {

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void ordemEhDoisElevadoAK(Modulation m) {
        assertEquals(1 << m.bitsPorSimbolo(), m.ordem());
        assertEquals(m.ordem(), m.constelacao().length);
    }

    @Test
    void valoresDeKEEficienciaEspectral() {
        assertEquals(1, Modulation.BPSK.bitsPorSimbolo());
        assertEquals(2, Modulation.QPSK.bitsPorSimbolo());
        assertEquals(4, Modulation.QAM16.bitsPorSimbolo());
        assertEquals(4.0, Modulation.QAM16.eficienciaEspectral(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void energiaMediaDeSimboloEhUm(Modulation m) {
        double soma = 0;
        for (Complex p : m.constelacao()) {
            soma += p.moduloQuadrado();
        }
        assertEquals(1.0, soma / m.ordem(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void vizinhosMaisProximosDiferemEmUmBit(Modulation m) {
        int n = m.ordem();
        for (int a = 0; a < n; a++) {
            // menor distância entre o símbolo a e qualquer outro
            double dMin = Double.MAX_VALUE;
            for (int b = 0; b < n; b++) {
                if (b != a) {
                    dMin = Math.min(dMin, m.simbolo(a).distancia(m.simbolo(b)));
                }
            }
            // todos os vizinhos nessa distância mínima devem diferir em 1 bit
            for (int b = 0; b < n; b++) {
                if (b != a && m.simbolo(a).distancia(m.simbolo(b)) < dMin + 1e-9) {
                    assertEquals(1, Integer.bitCount(a ^ b),
                            m.rotulo() + ": símbolos " + a + " e " + b + " não diferem em 1 bit");
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Modulation.class)
    void constelacaoTemPontosDistintos(Modulation m) {
        assertEquals(m.ordem(), new HashSet<>(Arrays.asList(m.constelacao())).size());
    }
}
