package br.unisinos.edu.experimento;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;

import java.util.HashSet;
import java.util.List;

/**
 * Parâmetros de um experimento. Valores repetidos são rejeitados, porque gerariam
 * linhas duplicadas no CSV sem aviso. p, r e fator valem para o canal de rajadas.
 */
public record ExperimentoConfig(
        List<Double> ebn0Db,
        int bits,
        List<Long> sementes,
        List<Modulation> modulacoes,
        List<LineCode> linhas,
        List<ChannelType> canais,
        List<Boolean> codigosHamming,
        double p,
        double r,
        double fator
) {
    public ExperimentoConfig {
        ebn0Db = List.copyOf(ebn0Db);
        sementes = List.copyOf(sementes);
        modulacoes = List.copyOf(modulacoes);
        linhas = List.copyOf(linhas);
        canais = List.copyOf(canais);
        codigosHamming = List.copyOf(codigosHamming);

        if (bits <= 0) {
            throw new IllegalArgumentException("bits deve ser positivo: " + bits);
        }
        if (ebn0Db.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos um valor em ebn0.valores");
        }
        for (double e : ebn0Db) {
            if (!Double.isFinite(e)) {
                throw new IllegalArgumentException("Eb/N0 não finito em ebn0.valores: " + e);
            }
        }
        if (sementes.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos uma semente em sementes");
        }
        if (modulacoes.isEmpty() && linhas.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos uma modulação ou código de linha");
        }
        if (canais.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos um canal em canais");
        }
        if (codigosHamming.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos um código em codigos");
        }

        semValoresRepetidos("ebn0.valores", ebn0Db);
        semValoresRepetidos("sementes", sementes);
        semValoresRepetidos("modulacoes", modulacoes);
        semValoresRepetidos("linhas", linhas);
        semValoresRepetidos("canais", canais);
        semValoresRepetidos("codigos", codigosHamming);

        if (!(p > 0.0 && p <= 1.0)) {
            throw new IllegalArgumentException("rajadas.p deve estar em (0, 1]: " + p);
        }
        if (!(r > 0.0 && r <= 1.0)) {
            throw new IllegalArgumentException("rajadas.r deve estar em (0, 1]: " + r);
        }
        if (!Double.isFinite(fator) || fator < 1.0) {
            throw new IllegalArgumentException("rajadas.fator deve ser finito e ≥ 1: " + fator);
        }
    }

    private static void semValoresRepetidos(String chave, List<?> valores) {
        if (new HashSet<>(valores).size() != valores.size()) {
            throw new IllegalArgumentException(chave + " tem valores repetidos: " + valores);
        }
    }
}