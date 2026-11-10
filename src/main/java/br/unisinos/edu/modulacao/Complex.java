package br.unisinos.edu.modulacao;

// re (eixo real, horizontal): chamado de I, de em fase.
// im (eixo imaginário, vertical): chamado de Q, de em quadratura.

public record Complex(
        double re,
        double im
) {

    public static final Complex ZERO = new Complex(0.0, 0.0);

    public Complex soma(Complex outro) {
        return new Complex(re + outro.re, im + outro.im);
    }

    public Complex multiplica(Complex outro) {
        return new Complex(re * outro.re - im * outro.im,
                re * outro.im + im * outro.re);
    }

    public Complex escala(double fator) {
        return new Complex(re * fator, im * fator);
    }

    public double moduloQuadrado() {
        return re * re + im * im;
    }

    public double modulo() {
        return Math.sqrt(moduloQuadrado());
    }

    public double distanciaQuadrada(Complex outro) {
        double dr = re - outro.re;
        double di = im - outro.im;
        return dr * dr + di * di;
    }

    public double distancia(Complex outro) {
        return Math.sqrt(distanciaQuadrada(outro));
    }

    public static Complex[] deReais(double[] valores) {
        Complex[] zs = new Complex[valores.length];
        for (int i = 0; i < valores.length; i++) {
            zs[i] = new Complex(valores[i], 0.0);
        }
        return zs;
    }

    public static double[] partesReais(Complex[] complex) {
        double[] re = new double[complex.length];
        for (int i = 0; i < complex.length; i++) {
            re[i] = complex[i].re();
        }
        return re;
    }
}