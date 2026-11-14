package br.unisinos.edu.metricas;

import br.unisinos.edu.canal.SnrConverter;
import org.apache.commons.math3.special.Erf;

public final class TheoreticalCurves {

    private TheoreticalCurves() {
    }

    /** Cauda da normal padrão: Q(x) = P(Z > x) = ½·erfc(x/√2). */
    public static double q(double x) {
        return 0.5 * Erf.erfc(x / Math.sqrt(2.0));
    }

    /** BPSK e QPSK, por bit, com Gray: Q(√(2·Eb/N0)). Entrada em dB. */
    public static double berBpskQpsk(double ebn0Db) {
        return q(Math.sqrt(2.0 * SnrConverter.dbParaLinear(ebn0Db)));
    }

    /** 16-QAM com Gray, aproximação: 0,75·Q(√(0,8·Eb/N0)). Entrada em dB. */
    public static double ber16Qam(double ebn0Db) {
        return 0.75 * q(Math.sqrt(0.8 * SnrConverter.dbParaLinear(ebn0Db)));
    }
}