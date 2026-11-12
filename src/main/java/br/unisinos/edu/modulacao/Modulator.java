package br.unisinos.edu.modulacao;

import java.util.Objects;

public class Modulator {
    private Modulator() {
    }

    /** Quantos zeros são acrescentados para completar o último símbolo. */
    public static int bitsDePadding(int tamanhoOriginal, Modulation m) {
        Objects.requireNonNull(m, "modulação não pode ser nula");
        if (tamanhoOriginal < 0) {
            throw new IllegalArgumentException("tamanhoOriginal negativo: " + tamanhoOriginal);
        }
        int k = m.bitsPorSimbolo();
        return (k - tamanhoOriginal % k) % k;
    }

    public static Complex[] modular(int[] bits, Modulation m) {
        Objects.requireNonNull(bits, "bits não pode ser nulo");
        Objects.requireNonNull(m, "modulação não pode ser nula");
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException(
                        "Valor inválido na posição " + i + ": " + bits[i]);
            }
        }

        int k = m.bitsPorSimbolo();
        int n = (bits.length + k - 1) / k;
        Complex[] simbolos = new Complex[n];
        for (int s = 0; s < n; s++) {
            int valor = 0;
            for (int b = 0; b < k; b++) {
                int i = s * k + b;
                int bit = i < bits.length ? bits[i] : 0; // padding com zeros
                valor = (valor << 1) | bit;              // MSB primeiro
            }
            simbolos[s] = m.simbolo(valor);
        }
        return simbolos;
    }
}
