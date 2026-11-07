package br.unisinos.edu.mensagem;

import java.util.Objects;
import java.util.Random;

public final class DataGenerator {

    private static final int BITS_POR_CARACTERE = 8;
    private static final int MAIOR_CARACTERE_ASCII = 127;

    private DataGenerator() {
    }


//     Converte texto ASCII em bits, 8 por caractere

    public static int[] textoParaBits(String texto) {
        Objects.requireNonNull(texto, "texto não pode ser nulo");
        int[] bits = new int[texto.length() * BITS_POR_CARACTERE];
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c > MAIOR_CARACTERE_ASCII) {
                throw new IllegalArgumentException(
                        "Caractere fora do ASCII na posição " + i + ": '" + c + "'");
            }
            for (int b = 0; b < BITS_POR_CARACTERE; b++) {
                // b = 0 é o bit mais significativo (MSB first)
                bits[i * BITS_POR_CARACTERE + b] = (c >> (BITS_POR_CARACTERE - 1 - b)) & 1;
            }
        }
        return bits;
    }

    public static String bitsParaTexto(int[] bits) {
        Objects.requireNonNull(bits, "bits não pode ser nulo");
        if (bits.length % BITS_POR_CARACTERE != 0) {
            throw new IllegalArgumentException(
                    "Quantidade de bits (" + bits.length + ") não é múltipla de 8");
        }
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException(
                        "Valor inválido na posição " + i + ": " + bits[i]);
            }
        }
        StringBuilder sb = new StringBuilder(bits.length / BITS_POR_CARACTERE);
        for (int i = 0; i < bits.length; i += BITS_POR_CARACTERE) {
            int valor = 0;
            for (int b = 0; b < BITS_POR_CARACTERE; b++) {
                valor = (valor << 1) | bits[i + b];
            }
            sb.append((char) valor);
        }
        return sb.toString();
    }

//    Mesma semente = mesma sequencia
    public static int[] bitsAleatorios(int n, long semente) {
        if (n < 0) {
            throw new IllegalArgumentException("n não pode ser negativo: " + n);
        }
        Random rnd = new Random(semente);
        int[] bits = new int[n];
        for (int i = 0; i < n; i++) {
            bits[i] = rnd.nextInt(2);
        }
        return bits;
    }
}
