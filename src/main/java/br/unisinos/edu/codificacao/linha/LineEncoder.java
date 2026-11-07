package br.unisinos.edu.codificacao.linha;

import java.util.Objects;

public final class LineEncoder {

    private LineEncoder() {
    }

    public static double[] codificar(int[] bits, LineCode codigo) {
        Objects.requireNonNull(bits, "bits não pode ser nulo");
        Objects.requireNonNull(codigo, "codigo não pode ser nulo");
        validarBits(bits);
        return switch (codigo) {
            case AMI -> codificarAmi(bits);
            case MANCHESTER -> codificarManchester(bits);
        };
    }

    private static double[] codificarAmi(int[] bits) {
        double[] amostras = new double[bits.length];
//         Polaridade que o próximo 1 deve assumir
//         Começa em +1.
        double proximaPolaridade = +1.0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 1) {
                amostras[i] = proximaPolaridade;
                proximaPolaridade = -proximaPolaridade;
            }
        }
        return amostras;
    }

    private static double[] codificarManchester(int[] bits) {
        double[] amostras = new double[bits.length * 2];
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 1) {
                amostras[2 * i] = +1.0;
                amostras[2 * i + 1] = -1.0;
            } else {
                amostras[2 * i] = -1.0;
                amostras[2 * i + 1] = +1.0;
            }
        }
        return amostras;
    }

    private static void validarBits(int[] bits) {
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException(
                        "Valor inválido na posição " + i + ": " + bits[i]);
            }
        }
    }
}