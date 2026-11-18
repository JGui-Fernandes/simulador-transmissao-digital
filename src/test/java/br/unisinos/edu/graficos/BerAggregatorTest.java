package br.unisinos.edu.graficos;

import br.unisinos.edu.graficos.BerAggregator.Curve;
import br.unisinos.edu.graficos.BerAggregator.Point;
import br.unisinos.edu.graficos.ResultReader.Run;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BerAggregatorTest {

    private static final long N = 10_000;

    private static Run run(String esquema, String canal, double ebn0, long semente, long erros) {
        return new Run(esquema, canal, "NENHUM", ebn0, semente, N, erros);
    }

    private static Point primeiroPonto(Map<Curve, List<Point>> curvas, String esquema, String canal) {
        return curvas.get(new Curve(esquema, canal, "NENHUM")).get(0);
    }

    @Test
    void mediaEIcBatemComCalculoManual() {
        // BER 0,01 e 0,02: média 0,015; desvio amostral = √0,00005;
        // meia-largura = t(1) · desvio / √2 = 12,706 · 0,005 = 0,06353
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("BPSK", "AWGN", 4.0, 1, 100),
                run("BPSK", "AWGN", 4.0, 2, 200)));

        Point p = primeiroPonto(curvas, "BPSK", "AWGN");
        assertEquals(2, p.ber().n());
        assertEquals(0.015, p.ber().media(), 1e-12);
        assertEquals(Math.sqrt(0.00005), p.ber().desvioPadrao(), 1e-12);
        assertEquals(-0.04853, p.ber().ic95Inferior(), 1e-9);
        assertEquals(0.07853, p.ber().ic95Superior(), 1e-9);
    }

    @Test
    void pisoSeAplicaPorExecucaoAntesDaMedia() {
        // N = 10.000 bits por execução.
        // Execução 1: 0 erros  -> BER = piso 1/N = 1e-4
        // Execução 2: 200 erros -> BER = 200/10.000 = 0,02
        // Média = (1e-4 + 0,02) / 2 = 0,01005
        // Se o piso fosse aplicado depois da média, daria 0,01.
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("QPSK", "AWGN", 6.0, 1, 0),
                run("QPSK", "AWGN", 6.0, 2, 200)));

        assertEquals(0.01005, primeiroPonto(curvas, "QPSK", "AWGN").ber().media(), 1e-12);
    }
    @Test
    void semErrosEmTodasAsSementesUsaOPisoUmSobreN() {
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("QAM16", "AWGN", 10.0, 1, 0),
                run("QAM16", "AWGN", 10.0, 2, 0)));

        Point p = primeiroPonto(curvas, "QAM16", "AWGN");
        assertEquals(1.0 / N, p.ber().media(), 1e-15);
        assertEquals(0.0, p.ber().desvioPadrao(), 1e-15);
    }

    @Test
    void awgnERajadasViramCurvasSeparadas() {
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("BPSK", "AWGN", 4.0, 1, 100),
                run("BPSK", "AWGN", 4.0, 2, 100),
                run("BPSK", "RAJADAS", 4.0, 1, 300),
                run("BPSK", "RAJADAS", 4.0, 2, 300)));

        assertEquals(2, curvas.size());
        assertEquals(0.01, primeiroPonto(curvas, "BPSK", "AWGN").ber().media(), 1e-12);
        assertEquals(0.03, primeiroPonto(curvas, "BPSK", "RAJADAS").ber().media(), 1e-12);
    }

    @Test
    void pontosSaoOrdenadosPorEbN0MesmoSeOCsvNaoEstiverOrdenado() {
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("AMI", "AWGN", 6.0, 1, 50), run("AMI", "AWGN", 6.0, 2, 50),
                run("AMI", "AWGN", 2.0, 1, 90), run("AMI", "AWGN", 2.0, 2, 90),
                run("AMI", "AWGN", 4.0, 1, 70), run("AMI", "AWGN", 4.0, 2, 70)));

        List<Point> pontos = curvas.get(new Curve("AMI", "AWGN", "NENHUM"));
        assertEquals(List.of(2.0, 4.0, 6.0),
                pontos.stream().map(Point::ebn0Db).toList());
    }

    @Test
    void sementeRepetidaFalha() {
        List<Run> runs = List.of(
                run("BPSK", "AWGN", 4.0, 1, 100),
                run("BPSK", "AWGN", 4.0, 1, 200));

        assertThrows(IllegalArgumentException.class, () -> BerAggregator.aggregate(runs));
    }

    @Test
    void umaSoSementeFalha() {
        List<Run> runs = List.of(run("BPSK", "AWGN", 4.0, 1, 100));

        assertThrows(IllegalArgumentException.class, () -> BerAggregator.aggregate(runs));
    }

    @Test
    void bitsMensagemDiferentesNoMesmoPontoFalham() {
        List<Run> runs = List.of(
                new Run("BPSK", "AWGN", "NENHUM", 4.0, 1, N, 100),
                new Run("BPSK", "AWGN", "NENHUM", 4.0, 2, N * 2, 100));

        assertThrows(IllegalArgumentException.class, () -> BerAggregator.aggregate(runs));
    }

    @Test
    void rotuloDaCurvaTemOsTresCampos() {
        assertEquals("QAM16 / RAJADAS / HAMMING74",
                new Curve("QAM16", "RAJADAS", "HAMMING74").label());
    }

    @Test
    void pontoContaExecucoesSemErros() {
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("QPSK", "AWGN", 6.0, 1, 0),
                run("QPSK", "AWGN", 6.0, 2, 0),
                run("QPSK", "AWGN", 6.0, 3, 200)));

        Point ponto = primeiroPonto(curvas, "QPSK", "AWGN");
        assertEquals(2, ponto.execucoesSemErros());
        assertTrue(ponto.temPiso());
    }

    @Test
    void pontoSemExecucoesSemErrosNaoTemPiso() {
        Map<Curve, List<Point>> curvas = BerAggregator.aggregate(List.of(
                run("QPSK", "AWGN", 6.0, 1, 5),
                run("QPSK", "AWGN", 6.0, 2, 200)));

        Point ponto = primeiroPonto(curvas, "QPSK", "AWGN");
        assertEquals(0, ponto.execucoesSemErros());
        assertFalse(ponto.temPiso());
    }
}