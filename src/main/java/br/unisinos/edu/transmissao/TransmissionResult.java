package br.unisinos.edu.transmissao;

import br.unisinos.edu.metricas.Metrics;

// bitsEnviados / bitsRecebidos: bits de informação, antes do Hamming e depois dele.
// bitsTransmitidos: bits que realmente passaram pelo canal
// eficienciaEspectral: nominal, em bits/s/Hz, já multiplicada pela taxa do código.
// sigma: desvio padrão por componente no estado bom do canal.
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
    public boolean mensagemRecuperada() {
        return ber.bitsErrados() == 0;
    }
}