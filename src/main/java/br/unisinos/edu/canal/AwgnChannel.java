package br.unisinos.edu.canal;

import br.unisinos.edu.modulacao.Complex;

import java.util.Objects;
import java.util.Random;

/**
 *  - bandaBase():   só a parte real recebe ruído (AMI e Manchester)
 *  - passaBanda():  I e Q recebem ruídos independentes (BPSK, QPSK, 16-QAM)
 */
public final class AwgnChannel implements Channel {

    private final boolean apenasParteReal;

    private AwgnChannel(boolean apenasParteReal) {
        this.apenasParteReal = apenasParteReal;
    }

    public static AwgnChannel bandaBase() {
        return new AwgnChannel(true);
    }

    public static AwgnChannel passaBanda() {
        return new AwgnChannel(false);
    }

    @Override
    public Complex[] apply(Complex[] sinal, double sigma, Random rnd) {
        Objects.requireNonNull(sinal, "sinal não pode ser nulo");
        Objects.requireNonNull(rnd, "rnd não pode ser nulo");
        if (sigma < 0) {
            throw new IllegalArgumentException("sigma não pode ser negativo: " + sigma);
        }

        Complex[] saida = new Complex[sinal.length];
        for (int i = 0; i < sinal.length; i++) {
            double ruidoRe = sigma * rnd.nextGaussian();
            double ruidoIm = apenasParteReal ? 0.0 : sigma * rnd.nextGaussian();
            saida[i] = new Complex(sinal[i].re() + ruidoRe, sinal[i].im() + ruidoIm);
        }
        return saida;
    }
}