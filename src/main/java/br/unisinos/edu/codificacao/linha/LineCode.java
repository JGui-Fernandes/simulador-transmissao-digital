package br.unisinos.edu.codificacao.linha;

public enum LineCode {

    AMI("AMI", 1),
    MANCHESTER("Manchester", 2);

    private final String rotulo;
    private final int amostrasPorBit;

    LineCode(String rotulo, int amostrasPorBit) {
        this.rotulo = rotulo;
        this.amostrasPorBit = amostrasPorBit;
    }

//    Nome exibido no gráfico
    public String rotulo() {
        return rotulo;
    }

    public int amostrasPorBit() {
        return amostrasPorBit;
    }
}
