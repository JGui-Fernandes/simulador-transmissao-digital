package br.unisinos.edu;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private static String capturarSaida(String... args) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.main(args);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void helpImprimeOUsoSemEncerrarAJvm() {
        String saida = capturarSaida("--help");
        assertTrue(saida.contains("Uso: java -jar gb.jar"), saida);
    }

    @Test
    void fluxoCompletoComSucessoEmSnrAlta() {
        String saida = capturarSaida("--mensagem", "Redes", "--modo", "MODULADO",
                "--modulacao", "QAM16", "--ebn0", "30");
        assertTrue(saida.contains("Mensagem recuperada: sim"), saida);
    }
}