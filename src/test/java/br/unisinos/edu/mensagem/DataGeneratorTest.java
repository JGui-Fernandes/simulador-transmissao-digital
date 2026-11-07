package br.unisinos.edu.mensagem;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DataGeneratorTest {

    @Test
    void letraAVirarBitsMsbPrimeiro() {
        // 'A' = 65 = 0100 0001
        assertArrayEquals(new int[] {0, 1, 0, 0, 0, 0, 0, 1},
                DataGenerator.textoParaBits("A"));
    }

    @Test
    void cadaCaractereGeraOitoBits() {
        assertEquals(5 * 8, DataGenerator.textoParaBits("Redes").length);
    }

    @Test
    void idaEVoltaComTextoDoTrabalho() {
        String texto = "Redes de Computadores - GB 2026!";
        assertEquals(texto, DataGenerator.bitsParaTexto(DataGenerator.textoParaBits(texto)));
    }

    @Test
    void idaEVoltaComTodosOsCaracteresAscii() {
        StringBuilder sb = new StringBuilder();
        for (char c = 0; c <= 127; c++) {
            sb.append(c);
        }
        String todos = sb.toString();
        assertEquals(todos, DataGenerator.bitsParaTexto(DataGenerator.textoParaBits(todos)));
    }

    @Test
    void textoVazioGeraSequenciaVazia() {
        assertEquals(0, DataGenerator.textoParaBits("").length);
        assertEquals("", DataGenerator.bitsParaTexto(new int[0]));
    }

    @Test
    void rejeitaCaracteresForaDoAscii() {
        // "ação" escrito com escapes Unicode para não depender da codificação do arquivo
        assertThrows(IllegalArgumentException.class,
                () -> DataGenerator.textoParaBits("a\u00e7\u00e3o"));
    }

    @Test
    void rejeitaQuantidadeDeBitsQueNaoFormaBytes() {
        assertThrows(IllegalArgumentException.class,
                () -> DataGenerator.bitsParaTexto(new int[10]));
    }

    @Test
    void rejeitaValoresDiferentesDeZeroEUm() {
        int[] bits = new int[8];
        bits[3] = 2;
        assertThrows(IllegalArgumentException.class,
                () -> DataGenerator.bitsParaTexto(bits));
    }

    @Test
    void bitsAleatoriosComMesmaSementeSaoIguais() {
        assertArrayEquals(
                DataGenerator.bitsAleatorios(1000, 42L),
                DataGenerator.bitsAleatorios(1000, 42L));
    }

    @Test
    void sementesDiferentesGeramSequenciasDiferentes() {
        assertFalse(Arrays.equals(
                DataGenerator.bitsAleatorios(1000, 1L),
                DataGenerator.bitsAleatorios(1000, 2L)));
    }

    @Test
    void bitsAleatoriosTemTamanhoValoresValidosEEquilibrio() {
        int[] bits = DataGenerator.bitsAleatorios(100_000, 7L);
        assertEquals(100_000, bits.length);
        assertTrue(Arrays.stream(bits).allMatch(b -> b == 0 || b == 1));
        double fracaoDeUns = Arrays.stream(bits).sum() / 100_000.0;
        assertEquals(0.5, fracaoDeUns, 0.01);
    }
}
