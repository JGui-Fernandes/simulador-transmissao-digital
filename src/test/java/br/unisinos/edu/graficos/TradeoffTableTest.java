package br.unisinos.edu.graficos;

import br.unisinos.edu.experimento.Statistics;
import br.unisinos.edu.graficos.BerAggregator.Curve;
import br.unisinos.edu.graficos.BerAggregator.Point;
import br.unisinos.edu.metricas.TheoreticalCurves;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TradeoffTableTest {

    private static Point ponto(double ebn0, double ber, int semErros) {
        return new Point(ebn0, 1_000_000, new Statistics.Resumo(5, ber, 0.0, ber, ber), semErros);
    }

    private static Point ponto(double ebn0, double ber) {
        return ponto(ebn0, ber, 0);
    }

    @Test
    void interpolaEmEscalaLogaritmica() {
        // 10⁻² em 0 dB e 10⁻⁴ em 2 dB: em log, 10⁻³ fica exatamente no meio.
        List<Point> p = List.of(ponto(0, 1e-2), ponto(2, 1e-4));
        assertEquals(1.0, TradeoffTable.cruzamento(p, 1e-3).orElseThrow().ebn0Db(), 1e-12);
    }

    @Test
    void indicaQuandoOAlvoFicaForaDaFaixa() {
        assertEquals("> 2", TradeoffTable.textoSimulado(List.of(ponto(0, 1e-1), ponto(2, 1e-2)), 1e-3));
        assertEquals("< 0", TradeoffTable.textoSimulado(List.of(ponto(0, 1e-5), ponto(2, 1e-6)), 1e-3));
    }

    @Test
    void marcaCruzamentoQueUsaPontoComPiso() {
        // log: −2 em 0 dB, −5 em 2 dB; −3 fica a 1/3 do caminho -> 0,67 dB
        List<Point> p = List.of(ponto(0, 1e-2), ponto(2, 1e-5, 3));
        assertEquals("0,7 *", TradeoffTable.textoSimulado(p, 1e-3));
    }

    @Test
    void teoricoDoBpskParaBer10aMenos3EhCercaDe6v8dB() {
        assertEquals(6.79, TradeoffTable.ebn0Teorico(TheoreticalCurves::berBpskQpsk, 1e-3), 0.01);
    }

    @Test
    void eficienciaConsideraATaxaDoHamming() {
        assertEquals(2.0, TradeoffTable.eficiencia(new Curve("QPSK", "AWGN", "NENHUM")), 1e-12);
        assertEquals(8.0 / 7.0, TradeoffTable.eficiencia(new Curve("QPSK", "AWGN", "HAMMING74")), 1e-12);
        assertEquals(0.5, TradeoffTable.eficiencia(new Curve("MANCHESTER", "AWGN", "NENHUM")), 1e-12);
    }

    @Test
    void gerarTemUmaLinhaPorCurva() {
        Map<Curve, List<Point>> curvas = new LinkedHashMap<>();
        curvas.put(new Curve("BPSK", "AWGN", "NENHUM"), List.of(ponto(6, 2.39e-3), ponto(8, 1.91e-4)));
        String md = TradeoffTable.gerar(curvas);
        assertTrue(md.contains("| BPSK | AWGN | nenhum | 1,000 |"), md);
        assertTrue(md.contains("10⁻³"), md);
    }
}
