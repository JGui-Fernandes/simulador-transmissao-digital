package br.unisinos.edu.metricas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Metrics {

    private Metrics() {
    }

    public record ResultadoBer(
            int totalBits,
            int bitsErrados,
            List<Integer>
            posicoesErradas
    ) {

//         BER = bits errados / bits totais
        public double taxa() {
            return (double) bitsErrados / totalBits;
        }
    }

    public static ResultadoBer ber(int[] enviados, int[] recebidos) {
        Objects.requireNonNull(enviados, "enviados não pode ser nulo");
        Objects.requireNonNull(recebidos, "recebidos não pode ser nulo");
        if (enviados.length != recebidos.length) {
            throw new IllegalArgumentException("Tamanhos diferentes: "
                    + enviados.length + " e " + recebidos.length);
        }
        if (enviados.length == 0) {
            throw new IllegalArgumentException("Sequência vazia: BER não é definida");
        }

        List<Integer> erros = new ArrayList<>();
        for (int i = 0; i < enviados.length; i++) {
            if (enviados[i] != recebidos[i]) {
                erros.add(i);
            }
        }
        return new ResultadoBer(enviados.length, erros.size(), List.copyOf(erros));
    }
}