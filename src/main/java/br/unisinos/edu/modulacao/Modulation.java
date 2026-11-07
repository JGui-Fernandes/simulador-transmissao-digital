package br.unisinos.edu.modulacao;

public enum Modulation {

    BPSK("BPSK", 1, construirBpsk()),
    QPSK("QPSK", 2, construirQpsk()),
    QAM16("16-QAM", 4, construirQam16());

    private final String rotulo;
    private final int bitsPorSimbolo;
    private final Complex[] constelacao;

    Modulation(String rotulo, int bitsPorSimbolo, Complex[] constelacao) {
        this.rotulo = rotulo;
        this.bitsPorSimbolo = bitsPorSimbolo;
        this.constelacao = constelacao;
    }

    public String rotulo() {
        return rotulo;
    }

    public int bitsPorSimbolo() {
        return bitsPorSimbolo;
    }

    public int ordem() {
        return constelacao.length;
    }

    public double eficienciaEspectral() {
        return bitsPorSimbolo;
    }

    public Complex simbolo(int valor) {
        return constelacao[valor];
    }

    public Complex[] constelacao() {
        return constelacao.clone();
    }

    private static Complex[] construirBpsk() {
        return new Complex[] {
                new Complex(-1.0, 0.0),
                new Complex(+1.0, 0.0)
        };
    }

    private static Complex[] construirQpsk() {
        double escala = 1.0 / Math.sqrt(2.0);
        Complex[] pts = new Complex[4];
        for (int s = 0; s < 4; s++) {
            double i = 2 * (s >> 1) - 1;   // bit mais significativo -> I
            double q = 2 * (s & 1) - 1;    // bit menos significativo -> Q
            pts[s] = new Complex(i * escala, q * escala);
        }
        return pts;
    }

    private static Complex[] construirQam16() {
        final int[] niveis = {-3, -1, 3, 1};
        double escala = 1.0 / Math.sqrt(10.0);   // energia média = 1
        Complex[] pts = new Complex[16];
        for (int s = 0; s < 16; s++) {
            double i = niveis[s >> 2];           // 2 bits mais significativos
            double q = niveis[s & 3];            // 2 bits menos significativos
            pts[s] = new Complex(i * escala, q * escala);
        }
        return pts;
    }
}