package br.unisinos.edu.codificacao.canal;
import java.util.Arrays;
import java.util.Objects;


public final class Hamming74 implements ChannelEncoder, ChannelDecoder {

    public static final int BITS_DADOS = 4;
    public static final int BITS_CODIGO = 7;
    public static final double TAXA = (double) BITS_DADOS / BITS_CODIGO;

    @Override
    public double taxa() {
        return TAXA;
    }

    public static int bitsDePadding(int tamanhoOriginal) {
        return (BITS_DADOS - tamanhoOriginal % BITS_DADOS) % BITS_DADOS;
    }

    @Override
    public int[] encode(int[] bits) {
        Objects.requireNonNull(bits, "bits não pode ser nulo");
        validarBits(bits);

        int blocos = (bits.length + BITS_DADOS - 1) / BITS_DADOS;
        int[] out = new int[blocos * BITS_CODIGO];

        for (int b = 0; b < blocos; b++) {
            int d1 = bit(bits, b * 4);
            int d2 = bit(bits, b * 4 + 1);
            int d3 = bit(bits, b * 4 + 2);
            int d4 = bit(bits, b * 4 + 3);

            int o = b * 7;
            out[o]     = d1 ^ d2 ^ d4; // p1
            out[o + 1] = d1 ^ d3 ^ d4; // p2
            out[o + 2] = d1;
            out[o + 3] = d2 ^ d3 ^ d4; // p3
            out[o + 4] = d2;
            out[o + 5] = d3;
            out[o + 6] = d4;
        }
        return out;
    }

    @Override
    public int[] decode(int[] coded, int originalLength) {
        Objects.requireNonNull(coded, "coded não pode ser nulo");
        validarBits(coded);
        if (coded.length % BITS_CODIGO != 0) {
            throw new IllegalArgumentException(
                    "Quantidade de bits (" + coded.length + ") não é múltipla de 7");
        }
        int blocos = coded.length / BITS_CODIGO;
        if (originalLength < 0 || (originalLength + BITS_DADOS - 1) / BITS_DADOS != blocos) {
            throw new IllegalArgumentException(
                    "originalLength " + originalLength + " incompatível com " + blocos + " blocos");
        }

        int[] dados = new int[blocos * BITS_DADOS];
        int[] c = new int[8]; // índices 1..7 para casar com as posições

        for (int b = 0; b < blocos; b++) {
            for (int i = 1; i <= 7; i++) c[i] = coded[b * 7 + i - 1];

            int s1 = c[1] ^ c[3] ^ c[5] ^ c[7];
            int s2 = c[2] ^ c[3] ^ c[6] ^ c[7];
            int s3 = c[4] ^ c[5] ^ c[6] ^ c[7];
            int sindrome = s1 + 2 * s2 + 4 * s3;

            if (sindrome != 0) c[sindrome] ^= 1;

            dados[b * 4]     = c[3];
            dados[b * 4 + 1] = c[5];
            dados[b * 4 + 2] = c[6];
            dados[b * 4 + 3] = c[7];
        }
        return Arrays.copyOf(dados, originalLength);
    }

    private static int bit(int[] v, int i) {
        return i < v.length ? v[i] : 0;
    }

    private static void validarBits(int[] bits) {
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException("Valor inválido na posição " + i + ": " + bits[i]);
            }
        }
    }
}