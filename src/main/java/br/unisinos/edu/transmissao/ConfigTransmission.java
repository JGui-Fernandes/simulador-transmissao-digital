package br.unisinos.edu.transmissao;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;

import java.util.Objects;

// modo LINHA: usa "linha" e ignora "modulacao"; modo MODULADO: o contrário.
// hamming: liga a codificação de canal Hamming(7,4), com taxa R = 4/7.
// p, r e fatorDegradacao só valem para o canal de rajadas (valores do cenário piloto).
public record ConfigTransmission(
        Mode modo,
        LineCode linha,
        Modulation modulacao,
        boolean hamming,
        ChannelType canal,
        double ebn0Db,
        long semente,
        double p,
        double r,
        double fatorDegradacao
) {
    public static final double P_PADRAO = 0.01;
    public static final double R_PADRAO = 0.1;
    public static final double FATOR_PADRAO = 5.0;

    public ConfigTransmission {
        Objects.requireNonNull(modo, "modo não pode ser nulo");
        Objects.requireNonNull(canal, "canal não pode ser nulo");
        if (modo == Mode.LINHA && linha == null) {
            throw new IllegalArgumentException("modo LINHA exige um código de linha");
        }
        if (modo == Mode.MODULADO && modulacao == null) {
            throw new IllegalArgumentException("modo MODULADO exige uma modulação");
        }
        if (!Double.isFinite(ebn0Db)) {
            throw new IllegalArgumentException("Eb/N0 inválido: " + ebn0Db);
        }
        if (!(p > 0.0 && p <= 1.0)) {
            throw new IllegalArgumentException("p deve estar em (0, 1]: " + p);
        }
        if (!(r > 0.0 && r <= 1.0)) {
            throw new IllegalArgumentException("r deve estar em (0, 1]: " + r);
        }
        if (!(fatorDegradacao >= 1.0) || !Double.isFinite(fatorDegradacao)) {
            throw new IllegalArgumentException("fatorDegradacao deve ser finito e ≥ 1: " + fatorDegradacao);
        }
    }

    public static ConfigTransmission paraLinha(LineCode linha, boolean hamming, ChannelType canal,
                                               double ebn0Db, long semente) {
        return new ConfigTransmission(Mode.LINHA, linha, null, hamming, canal, ebn0Db, semente,
                P_PADRAO, R_PADRAO, FATOR_PADRAO);
    }

    public static ConfigTransmission paraModulacao(Modulation modulacao, boolean hamming, ChannelType canal,
                                                   double ebn0Db, long semente) {
        return new ConfigTransmission(Mode.MODULADO, null, modulacao, hamming, canal, ebn0Db, semente,
                P_PADRAO, R_PADRAO, FATOR_PADRAO);
    }

    /** Cópia com outros parâmetros do canal de rajadas. */
    public ConfigTransmission comRajadas(double novoP, double novoR, double novoFator) {
        return new ConfigTransmission(modo, linha, modulacao, hamming, canal, ebn0Db, semente,
                novoP, novoR, novoFator);
    }
}