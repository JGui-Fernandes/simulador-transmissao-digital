package br.unisinos.edu.codificacao.linha;

public enum LineCode {
    AMI("AMI", 1, 1.0),
    MANCHESTER("Manchester", 2, 0.5);

    private final String rotulo;
    private final int amostrasPorBit;
    private final double eficienciaEspectral;

    LineCode(String rotulo, int amostrasPorBit, double eficienciaEspectral) {
        this.rotulo = rotulo;
        this.amostrasPorBit = amostrasPorBit;
        this.eficienciaEspectral = eficienciaEspectral;
    }

    // Nome exibido no gráfico
    public String rotulo() {
        return rotulo;
    }

    public int amostrasPorBit() {
        return amostrasPorBit;
    }

    /**
     * Eficiência espectral nominal em bits/s/Hz, com a banda medida do zero ao
     * primeiro nulo do espectro: AMI ocupa cerca de Rb (1 bit/s/Hz) e Manchester
     * cerca de 2·Rb (0,5 bit/s/Hz), porque cada bit usa duas amostras.
     */
    public double eficienciaEspectral() {
        return eficienciaEspectral;
    }
}