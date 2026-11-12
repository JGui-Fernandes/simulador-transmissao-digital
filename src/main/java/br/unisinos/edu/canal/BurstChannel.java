package br.unisinos.edu.canal;

import br.unisinos.edu.modulacao.Complex;

import java.util.Objects;
import java.util.Random;


//  Estado bom: ruído gaussiano com o sigma recebido.
//  Estado ruim: ruído gaussiano com sigma * fatorDegradacao.
//  p: probabilidade de ir para o ruim, por amostra.
//  r: probabilidade de ir para o bom, por amostra.

public final class BurstChannel implements Channel {

    private final double p;
    private final double r;
    private final double fatorDegradacao;
    private final boolean apenasParteReal;

    private boolean estadoRuim;
    private boolean[] estadosUltimaChamada = new boolean[0];

    private BurstChannel(double p, double r, double fatorDegradacao, boolean apenasParteReal) {
        validarProbabilidade(p, "p");
        validarProbabilidade(r, "r");
        if (!(fatorDegradacao >= 1.0) || !Double.isFinite(fatorDegradacao)) {
            throw new IllegalArgumentException(
                    "fatorDegradacao deve ser finito e ≥ 1: " + fatorDegradacao);
        }
        this.p = p;
        this.r = r;
        this.fatorDegradacao = fatorDegradacao;
        this.apenasParteReal = apenasParteReal;
        this.estadoRuim = false;
    }

    public static BurstChannel bandaBase(double p, double r, double fatorDegradacao) {
        return new BurstChannel(p, r, fatorDegradacao, true);
    }

    public static BurstChannel passaBanda(double p, double r, double fatorDegradacao) {
        return new BurstChannel(p, r, fatorDegradacao, false);
    }

    @Override
    public Complex[] apply(Complex[] sinal, double sigma, Random rnd) {
        Objects.requireNonNull(sinal, "sinal não pode ser nulo");
        Objects.requireNonNull(rnd, "rnd não pode ser nulo");
        if (!(sigma >= 0) || !Double.isFinite(sigma)) {
            throw new IllegalArgumentException("sigma inválido: " + sigma);
        }

        double sigmaRuim = sigma * fatorDegradacao;
        Complex[] saida = new Complex[sinal.length];
        boolean[] estados = new boolean[sinal.length];

        for (int i = 0; i < sinal.length; i++) {
            avancarEstado(rnd);
            estados[i] = estadoRuim;

            double s = estadoRuim ? sigmaRuim : sigma;
            double ruidoRe = s * rnd.nextGaussian();
            double ruidoIm = apenasParteReal ? 0.0 : s * rnd.nextGaussian();
            saida[i] = new Complex(sinal[i].re() + ruidoRe, sinal[i].im() + ruidoIm);
        }

        estadosUltimaChamada = estados;
        return saida;
    }

    private void avancarEstado(Random rnd) {
        double u = rnd.nextDouble();
        if (estadoRuim) {
            if (u < r) {
                estadoRuim = false;
            }
        } else if (u < p) {
            estadoRuim = true;
        }
    }

    public void reiniciar() {
        estadoRuim = false;
        estadosUltimaChamada = new boolean[0];
    }

    public boolean estaNoEstadoRuim() {
        return estadoRuim;
    }

    public boolean[] estadosUltimaChamada() {
        return estadosUltimaChamada.clone();
    }

    public double fracaoEsperadaRuim() {
        return p / (p + r);
    }

    public double duracaoMediaRajada() {
        return 1.0 / r;
    }

    public double potenciaRuidoRelativa() {
        double pi = fracaoEsperadaRuim();
        return (1.0 - pi) + pi * fatorDegradacao * fatorDegradacao;
    }

    public double sigmaBomParaMesmaPotenciaMedia(double sigmaMedio) {
        if (!(sigmaMedio >= 0) || !Double.isFinite(sigmaMedio)) {
            throw new IllegalArgumentException("sigmaMedio inválido: " + sigmaMedio);
        }
        return sigmaMedio / Math.sqrt(potenciaRuidoRelativa());
    }

    public double p() {
        return p;
    }

    public double r() {
        return r;
    }

    public double fatorDegradacao() {
        return fatorDegradacao;
    }

    private static void validarProbabilidade(double valor, String nome) {
        if (!(valor > 0.0 && valor <= 1.0)) {
            throw new IllegalArgumentException(nome + " deve estar em (0, 1]: " + valor);
        }
    }
}