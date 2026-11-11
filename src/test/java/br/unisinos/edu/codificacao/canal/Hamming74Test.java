package br.unisinos.edu.codificacao.canal;

import br.unisinos.edu.mensagem.DataGenerator;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class Hamming74Test {
    private final Hamming74 h = new Hamming74();

    private static int[] bloco(int v) {
        return new int[] {(v >> 3) & 1, (v >> 2) & 1, (v >> 1) & 1, v & 1};
    }

    // ---------- Sem ruído ----------

    @Test
    void os16BlocosSaoRecuperadosSemRuido() {
        for (int v = 0; v < 16; v++) {
            int[] cod = h.encode(bloco(v));
            assertEquals(7, cod.length);
            assertArrayEquals(bloco(v), h.decode(cod, 4), "bloco " + v);
        }
    }

    @Test
    void distanciaMinimaEntrePalavrasCodigoEhTres() {
        int dMin = Integer.MAX_VALUE;
        for (int a = 0; a < 16; a++) {
            for (int b = a + 1; b < 16; b++) {
                int[] ca = h.encode(bloco(a));
                int[] cb = h.encode(bloco(b));
                int d = 0;
                for (int i = 0; i < 7; i++) d += ca[i] ^ cb[i];
                dMin = Math.min(dMin, d);
            }
        }
        assertEquals(3, dMin);
    }

    // ---------- Correção ----------

    @Test
    void todoErroSimplesEhCorrigido() {
        for (int v = 0; v < 16; v++) {
            int[] cod = h.encode(bloco(v));
            for (int pos = 0; pos < 7; pos++) {
                int[] comErro = cod.clone();
                comErro[pos] ^= 1;
                assertArrayEquals(bloco(v), h.decode(comErro, 4),
                        "bloco " + v + ", erro na posição " + (pos + 1));
            }
        }
    }

    @Test
    void erroDuploNoMesmoBlocoNaoEhCorrigido() {

        for (int v = 0; v < 16; v++) {
            int[] cod = h.encode(bloco(v));
            for (int i = 0; i < 7; i++) {
                for (int j = i + 1; j < 7; j++) {
                    int[] comErro = cod.clone();
                    comErro[i] ^= 1;
                    comErro[j] ^= 1;
                    assertFalse(Arrays.equals(bloco(v), h.decode(comErro, 4)),
                            "bloco " + v + ", erros em " + (i + 1) + " e " + (j + 1));
                }
            }
        }
    }

    // ---------- Padding ----------

    @Test
    void paddingQuandoTamanhoNaoEhMultiploDe4() {
        int[] bits = {1, 0, 1, 1, 0, 1};
        int[] cod = h.encode(bits);
        assertEquals(14, cod.length);
        assertEquals(2, Hamming74.bitsDePadding(bits.length));
        assertArrayEquals(bits, h.decode(cod, bits.length));
    }

    @Test
    void idaEVoltaComTextoEBitsAleatorios() {
        String texto = "Redes de Computadores - GB 2026!";
        int[] bitsTexto = DataGenerator.textoParaBits(texto);
        assertEquals(texto, DataGenerator.bitsParaTexto(
                h.decode(h.encode(bitsTexto), bitsTexto.length)));

        int[] aleatorios = DataGenerator.bitsAleatorios(100_001, 9L);
        assertArrayEquals(aleatorios, h.decode(h.encode(aleatorios), aleatorios.length));
    }

    // ---------- Validação ----------

    @Test
    void taxaEhQuatroSetimos() {
        assertEquals(4.0 / 7.0, h.taxa(), 1e-12);
    }

    @Test
    void rejeitaEntradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> h.encode(new int[] {0, 2}));
        assertThrows(IllegalArgumentException.class, () -> h.decode(new int[8], 4));
        assertThrows(IllegalArgumentException.class, () -> h.decode(new int[7], 5));
        assertThrows(IllegalArgumentException.class, () -> h.decode(new int[7], -1));
        assertThrows(NullPointerException.class, () -> h.encode(null));
    }
}