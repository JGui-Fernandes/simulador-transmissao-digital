package br.unisinos.edu.transmissao;

import br.unisinos.edu.metricas.Metrics;

import java.util.Objects;

public record TransmissionResult(
        ConfigTransmission config,
        int[] bitsEnviados,
        int[] bitsRecebidos,
        Metrics.ResultadoBer ber,
        int bitsTransmitidos,
        double taxaCodigo,
        double eficienciaEspectral,
        double sigma
) {
    public TransmissionResult {
        Objects.requireNonNull(config, "config não pode ser nula");
        Objects.requireNonNull(ber, "ber não pode ser nulo");
        bitsEnviados = Objects.requireNonNull(bitsEnviados, "bitsEnviados não pode ser nulo").clone();
        bitsRecebidos = Objects.requireNonNull(bitsRecebidos, "bitsRecebidos não pode ser nulo").clone();
    }

    @Override
    public int[] bitsEnviados() {
        return bitsEnviados.clone();
    }

    @Override
    public int[] bitsRecebidos() {
        return bitsRecebidos.clone();
    }

    public boolean mensagemRecuperada() {
        return ber.bitsErrados() == 0;
    }
}