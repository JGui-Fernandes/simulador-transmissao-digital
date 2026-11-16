package br.unisinos.edu.experimento;

import java.util.Objects;
import java.util.TreeMap;

/** Média, desvio padrão amostral e intervalo de confiança de 95% pela distribuição t. */
public final class Statistics {

    /** Valores de t para 95% bicaudal, indexados pelos graus de liberdade. */
    private static final TreeMap<Integer, Double> T_95 = new TreeMap<>();

    static {
        T_95.put(1, 12.706);
        T_95.put(2, 4.303);
        T_95.put(3, 3.182);
        T_95.put(4, 2.776);
        T_95.put(5, 2.571);
        T_95.put(6, 2.447);
        T_95.put(7, 2.365);
        T_95.put(8, 2.306);
        T_95.put(9, 2.262);
        T_95.put(10, 2.228);
        T_95.put(15, 2.131);
        T_95.put(20, 2.086);
        T_95.put(30, 2.042);
    }

    private Statistics() {
    }

    public record Resumo(int n, double media, double desvioPadrao,
                         double ic95Inferior, double ic95Superior) {
        public double meiaLargura() {
            return (ic95Superior - ic95Inferior) / 2.0;
        }
    }

    /**
     * Valor de t para 95% com os graus de liberdade dados. Para gl não tabelado usa
     * o valor do maior gl tabelado que não o supera: é conservador (t maior, IC mais largo).
     */
    public static double tCritico95(int grausLiberdade) {
        if (grausLiberdade < 1) {
            throw new IllegalArgumentException("graus de liberdade devem ser ≥ 1: " + grausLiberdade);
        }
        return T_95.floorEntry(grausLiberdade).getValue();
    }

    public static Resumo resumir(double[] valores) {
        Objects.requireNonNull(valores, "valores não pode ser nulo");
        int n = valores.length;
        if (n < 2) {
            throw new IllegalArgumentException("são necessárias pelo menos 2 repetições: " + n);
        }
        double soma = 0.0;
        for (double v : valores) {
            soma += v;
        }
        double media = soma / n;

        double somaQuadrados = 0.0;
        for (double v : valores) {
            double d = v - media;
            somaQuadrados += d * d;
        }
        double desvio = Math.sqrt(somaQuadrados / (n - 1));
        double meiaLargura = tCritico95(n - 1) * desvio / Math.sqrt(n);

        return new Resumo(n, media, desvio, media - meiaLargura, media + meiaLargura);
    }
}