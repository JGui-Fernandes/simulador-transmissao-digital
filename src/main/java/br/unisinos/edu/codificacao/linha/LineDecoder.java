package br.unisinos.edu.codificacao.linha;

import java.util.Objects;

public final class LineDecoder {

    private static final double LIMIAR_AMI = 0.5;

    private LineDecoder() {
    }

    public static int[] decodificar(double[] amostras, LineCode codigo) {
        Objects.requireNonNull(amostras, "amostras não pode ser nulo");
        Objects.requireNonNull(codigo, "codigo não pode ser nulo");

        int porBit = codigo.amostrasPorBit();
        if (amostras.length % porBit != 0) {
            throw new IllegalArgumentException(
                    "Quantidade de amostras (" + amostras.length
                            + ") não é múltipla de " + porBit + " para " + codigo.rotulo());
        }
        return switch (codigo) {
            case AMI -> decodificarAmi(amostras);
            case MANCHESTER -> decodificarManchester(amostras);
        };
    }

    private static int[] decodificarAmi(double[] amostras) {
        int[] bits = new int[amostras.length];
        for (int i = 0; i < amostras.length; i++) {
            bits[i] = Math.abs(amostras[i]) > LIMIAR_AMI ? 1 : 0;
        }
        return bits;
    }

    private static int[] decodificarManchester(double[] amostras) {
        int n = amostras.length / 2;
        int[] bits = new int[n];
        for (int i = 0; i < n; i++) {
            double diferenca = amostras[2 * i] - amostras[2 * i + 1];
            bits[i] = diferenca > 0 ? 1 : 0;
        }
        return bits;
    }
}