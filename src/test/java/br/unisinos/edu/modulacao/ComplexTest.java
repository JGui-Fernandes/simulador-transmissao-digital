package br.unisinos.edu.modulacao;

import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ComplexTest {

    @Test
    void somaComponentes() {
        // (1+2j) + (3-4j) = 4-2j
        assertEquals(new Complex(4, -2), new Complex(1, 2).soma(new Complex(3, -4)));
    }

    @Test
    void multiplicaSeguindoAFormulaComplexa() {
        // (1+2j)(3-j) = 3 - j + 6j - 2j² = 5 + 5j
        assertEquals(new Complex(5, 5), new Complex(1, 2).multiplica(new Complex(3, -1)));
    }

    @Test
    void jAoQuadradoEhMenosUm() {
        assertEquals(new Complex(-1, 0), new Complex(0, 1).multiplica(new Complex(0, 1)));
    }

    @Test
    void escalaMultiplicaAsDuasComponentes() {
        assertEquals(new Complex(1.5, -3), new Complex(3, -6).escala(0.5));
    }

    @Test
    void moduloQuadradoDeTresMaisQuatroJ() {
        assertEquals(25.0, new Complex(3, 4).moduloQuadrado(), 1e-12);
        assertEquals(5.0, new Complex(3, 4).modulo(), 1e-12);
    }

    @Test
    void distanciaEntrePontos() {
        Complex origem = Complex.ZERO;
        Complex ponto = new Complex(3, 4);
        assertEquals(25.0, origem.distanciaQuadrada(ponto), 1e-12);
        assertEquals(5.0, origem.distancia(ponto), 1e-12);
    }

    @Test
    void conversaoDeRealEIdaEVolta() {
        double[] valores = {1.5, -0.25, 0.0};
        Complex[] zs = Complex.deReais(valores);
        assertEquals(0.0, zs[0].im(), 0.0);
        assertArrayEquals(valores, Complex.partesReais(zs), 0.0);
    }
}
