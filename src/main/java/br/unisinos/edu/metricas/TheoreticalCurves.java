package br.unisinos.edu.metricas;

import br.unisinos.edu.canal.SnrConverter;
import org.apache.commons.math3.special.Erf;

public final class TheoreticalCurves {

    private TheoreticalCurves() {
    }

//    Cauda da normal padrão
    public static double q(double x) {
        return 0.5 * Erf.erfc(x / Math.sqrt(2.0));
    }

//    BPSK e QPSK, por bit, com Gray
    public static double berBpskQpsk(double ebn0Db) {
        return q(Math.sqrt(2.0 * SnrConverter.dbParaLinear(ebn0Db)));
    }

//    16-QAM com Gray
    public static double ber16Qam(double ebn0Db) {
        return 0.75 * q(Math.sqrt(0.8 * SnrConverter.dbParaLinear(ebn0Db)));
    }
}