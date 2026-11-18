package br.unisinos.edu.graficos;

import br.unisinos.edu.experimento.Statistics;
import br.unisinos.edu.graficos.ResultReader.Run;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Agrupa as execuções em curvas (esquema, canal e código de canal) e calcula, para cada
 * ponto de Eb/N0, a média da BER e o IC 95% entre as sementes.
 *
 * O piso 1/N é aplicado a cada execução sem erros, antes da média. Assim, uma execução
 * sem erros não zera a média do ponto. A BER é recalculada a partir das contagens,
 * não a partir da coluna {@code ber} do CSV.
 */
public final class BerAggregator {

    private BerAggregator() {
    }

    /** Identifica uma curva. */
    public record Curve(String scheme, String channel, String code) {

        /** Rótulo curto, usado em mensagens de erro e na legenda. */
        public String label() {
            return scheme + " / " + channel + " / " + code;
        }
    }

    /**
     * Um ponto da curva: BER média com IC 95% entre as sementes, em um Eb/N0.
     *
     * @param execucoesSemErros quantas execuções deste ponto tiveram zero erros. Essas execuções
     *                          entram na média com o piso 1/N. Se for maior que zero, o ponto
     *                          depende do piso e deve ser marcado nas figuras.
     */
    public record Point(double ebn0Db, long messageBits, Statistics.Resumo ber, int execucoesSemErros) {

        public Point {
            if (execucoesSemErros < 0) {
                throw new IllegalArgumentException("execucoesSemErros não pode ser negativo: " + execucoesSemErros);
            }
        }

        /** Indica se o ponto depende do piso 1/N em pelo menos uma execução. */
        public boolean temPiso() {
            return execucoesSemErros > 0;
        }
    }

    /**
     * Agrupa as execuções. As curvas saem na ordem em que aparecem no CSV,
     * e os pontos de cada curva em ordem crescente de Eb/N0.
     */
    public static Map<Curve, List<Point>> aggregate(List<Run> runs) {
        Objects.requireNonNull(runs, "runs não pode ser nulo");

        Map<Curve, TreeMap<Double, List<Run>>> grouped = new LinkedHashMap<>();
        for (Run run : runs) {
            Curve curve = new Curve(run.scheme(), run.channel(), run.code());
            grouped.computeIfAbsent(curve, k -> new TreeMap<>())
                    .computeIfAbsent(run.ebn0Db(), k -> new ArrayList<>())
                    .add(run);
        }

        Map<Curve, List<Point>> curves = new LinkedHashMap<>();
        for (Map.Entry<Curve, TreeMap<Double, List<Run>>> curve : grouped.entrySet()) {
            List<Point> points = new ArrayList<>();
            for (Map.Entry<Double, List<Run>> point : curve.getValue().entrySet()) {
                points.add(pointOf(curve.getKey(), point.getKey(), point.getValue()));
            }
            curves.put(curve.getKey(), points);
        }
        return curves;
    }

    private static Point pointOf(Curve curve, double ebn0Db, List<Run> runs) {
        String onde = curve.label() + " em Eb/N0=" + ebn0Db + " dB";
        if (runs.size() < 2) {
            throw new IllegalArgumentException("são necessárias pelo menos 2 sementes: " + onde);
        }

        long messageBits = runs.get(0).messageBits();
        Set<Long> seeds = new HashSet<>();
        double[] bers = new double[runs.size()];
        int semErros = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            if (run.messageBits() != messageBits) {
                throw new IllegalArgumentException("bits_mensagem diferentes no mesmo ponto: " + onde);
            }
            if (!seeds.add(run.seed())) {
                throw new IllegalArgumentException("semente repetida (" + run.seed() + "): " + onde);
            }
            if (run.bitErrors() == 0) {
                semErros++;
            }
            bers[i] = berOf(run);
        }
        return new Point(ebn0Db, messageBits, Statistics.resumir(bers), semErros);
    }

    /** BER de uma execução. Sem erros, usa o piso 1/N. */
    static double berOf(Run run) {
        if (run.bitErrors() == 0) {
            return 1.0 / run.messageBits();
        }
        return (double) run.bitErrors() / run.messageBits();
    }
}