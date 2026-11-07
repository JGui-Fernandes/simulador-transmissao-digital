package br.unisinos.edu.codificacao.linha;

import br.unisinos.edu.mensagem.DataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class LineDecoderTest {

    // ---------- Ida e volta sem ruído ----------

    @ParameterizedTest
    @EnumSource(LineCode.class)
    void idaEVoltaComBitsAleatorios(LineCode codigo) {
        int[] bits = DataGenerator.bitsAleatorios(100_000, 10L);
        int[] recuperados = LineDecoder.decodificar(
                LineEncoder.codificar(bits, codigo), codigo);
        assertArrayEquals(bits, recuperados);
    }

    @ParameterizedTest
    @EnumSource(LineCode.class)
    void idaEVoltaComTextoDoTrabalho(LineCode codigo) {
        String texto = "Redes de Computadores - GB 2026!";
        int[] bits = DataGenerator.textoParaBits(texto);
        int[] recuperados = LineDecoder.decodificar(
                LineEncoder.codificar(bits, codigo), codigo);
        assertEquals(texto, DataGenerator.bitsParaTexto(recuperados));
    }

    @ParameterizedTest
    @EnumSource(LineCode.class)
    void idaEVoltaComTodosOsCaracteresAscii(LineCode codigo) {
        StringBuilder sb = new StringBuilder();
        for (char c = 0; c <= 127; c++) {
            sb.append(c);
        }
        String todos = sb.toString();
        int[] recuperados = LineDecoder.decodificar(
                LineEncoder.codificar(DataGenerator.textoParaBits(todos), codigo), codigo);
        assertEquals(todos, DataGenerator.bitsParaTexto(recuperados));
    }

    // ---------- Regras de decisão ----------

    @Test
    void amiDecodificaPorMagnitude() {
        // +1 -> 1, 0 -> 0, -1 -> 1 (a polaridade não importa na decisão)
        assertArrayEquals(new int[] {1, 0, 1},
                LineDecoder.decodificar(new double[] {+1, 0, -1}, LineCode.AMI));
    }

    @Test
    void amiTolerarRuidoAteOLimiarDe0_5() {
        // 0,6 ainda é um pulso; 0,4 ainda é zero
        assertArrayEquals(new int[] {1, 0, 1, 0},
                LineDecoder.decodificar(new double[] {0.6, 0.4, -0.7, -0.3}, LineCode.AMI));
    }

    @Test
    void manchesterComparaAsDuasMetades() {
        // (+1,-1) -> 1;  (-1,+1) -> 0
        assertArrayEquals(new int[] {1, 0},
                LineDecoder.decodificar(new double[] {+1, -1, -1, +1}, LineCode.MANCHESTER));
    }

    @Test
    void manchesterTolerDeslocamentoDCComumAsDuasMetades() {
        // Deslocamento DC de +1,2 aplicado às duas metades.
        // Bit 0 vira (0,2; 2,2): a primeira amostra sozinha seria positiva e decidiria 1 (erro).
        // A diferença r1 - r2 = -2 continua correta e decide 0.
        double dc = 1.2;
        double[] recebidas = {
                +1 + dc, -1 + dc,    // bit 1
                -1 + dc, +1 + dc     // bit 0
        };
        assertArrayEquals(new int[] {1, 0},
                LineDecoder.decodificar(recebidas, LineCode.MANCHESTER));
    }

    // ---------- Validação de entrada ----------

    @Test
    void manchesterRejeitaNumeroImparDeAmostras() {
        assertThrows(IllegalArgumentException.class,
                () -> LineDecoder.decodificar(new double[3], LineCode.MANCHESTER));
    }

    @Test
    void rejeitaEntradaNula() {
        assertThrows(NullPointerException.class,
                () -> LineDecoder.decodificar(null, LineCode.AMI));
    }
}
