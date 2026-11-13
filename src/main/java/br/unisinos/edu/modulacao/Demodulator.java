package br.unisinos.edu.modulacao;

import java.util.Arrays;
import java.util.Objects;

public class Demodulator {
    private Demodulator() {
    }

    public static int[] demodular(Complex[] recebidos, Modulation m, int tamanhoOriginal) {
        Objects.requireNonNull(recebidos, "recebidos não pode ser nulo");
        Objects.requireNonNull(m, "modulação não pode ser nula");

        int k = m.bitsPorSimbolo();
        if (tamanhoOriginal < 0 || (tamanhoOriginal + k - 1) / k != recebidos.length) {
            throw new IllegalArgumentException("tamanhoOriginal " + tamanhoOriginal
                    + " incompatível com " + recebidos.length + " símbolos");
        }

        // constelacao() devolve um clone: chamar uma vez, fora do laço
        Complex[] constelacao = m.constelacao();
        int[] bits = new int[recebidos.length * k];

        for (int s = 0; s < recebidos.length; s++) {

            // NaN e infinito nunca vencem a comparação "d < menor": sem esta checagem,
            // o símbolo seria decodificado como o padrão 0 sem avisar.
            if (!Double.isFinite(recebidos[s].re()) || !Double.isFinite(recebidos[s].im())) {
                throw new IllegalArgumentException(
                        "Símbolo recebido inválido na posição " + s + ": " + recebidos[s]);
            }
            int melhor = 0;
            double menor = Double.MAX_VALUE;
            for (int p = 0; p < constelacao.length; p++) {
                double d = recebidos[s].distanciaQuadrada(constelacao[p]);
                if (d < menor) {
                    menor = d;
                    melhor = p;
                }
            }
            for (int b = 0; b < k; b++) {
                bits[s * k + b] = (melhor >> (k - 1 - b)) & 1; // MSB primeiro
            }
        }
        return Arrays.copyOf(bits, tamanhoOriginal);
    }

}
