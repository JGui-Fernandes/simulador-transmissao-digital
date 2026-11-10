package br.unisinos.edu.canal;

import br.unisinos.edu.codificacao.linha.LineCode;

public final class SnrConverter {

    private SnrConverter() {
    }

    public static double dbParaLinear(double db) {
        return Math.pow(10.0, db / 10.0);
    }

    public static double linearParaDb(double linear) {
        return 10.0 * Math.log10(linear);
    }

    //    EB = Energia por bit
    //    ES = Energia por símbolo
    //    DB = decibeis
    public static double ebn0ParaEsn0Db(double ebn0Db, int k, double taxaCodigo) {
        validarParametros(k, taxaCodigo);
        return ebn0Db + linearParaDb(k * taxaCodigo);
    }

    //    EB = Energia por bit
    //    ES = Energia por símbolo
    //    DB = decibeis
    public static double esn0ParaEbn0Db(double esn0Db, int k, double taxaCodigo) {
        validarParametros(k, taxaCodigo);
        return esn0Db - linearParaDb(k * taxaCodigo);
    }

    public static double sigmaPassaBanda(double esn0Db) {
        return Math.sqrt(1.0 / (2.0 * dbParaLinear(esn0Db)));
    }

    public static double energiaPorBitBandaBase(LineCode codigo) {
        double potenciaMediaPorAmostra = switch (codigo) {
            case AMI -> 0.5;
            case MANCHESTER -> 1.0;
        };
        return codigo.amostrasPorBit() * potenciaMediaPorAmostra;
    }

    public static double sigmaBandaBase(double ebn0Db, double energiaPorBit, double taxaCodigo) {
        if (energiaPorBit <= 0) {
            throw new IllegalArgumentException("energiaPorBit deve ser positiva: " + energiaPorBit);
        }
        if (taxaCodigo <= 0 || taxaCodigo > 1) {
            throw new IllegalArgumentException("taxa do código fora de (0, 1]: " + taxaCodigo);
        }
        return Math.sqrt(energiaPorBit / (2.0 * taxaCodigo * dbParaLinear(ebn0Db)));
    }

    private static void validarParametros(int k, double taxaCodigo) {
        if (k < 1) {
            throw new IllegalArgumentException("k deve ser ≥ 1: " + k);
        }
        if (taxaCodigo <= 0 || taxaCodigo > 1) {
            throw new IllegalArgumentException("taxa do código fora de (0, 1]: " + taxaCodigo);
        }
    }
}
