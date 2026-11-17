package br.unisinos.edu.graficos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlotGeneratorTest {

    private static final String CABECALHO =
            "esquema,canal,codigo,ebn0_db,semente,bits_mensagem,bits_errados\n";

    @Test
    void geraAsQuatroFigurasComPngValido(@TempDir Path tmp) throws IOException {
        Path csv = tmp.resolve("teste.csv");
        Files.writeString(csv, CABECALHO + linhasCompletas(), StandardCharsets.UTF_8);

        List<Path> arquivos = PlotGenerator.gerarTodas(csv, tmp.resolve("figuras"));

        assertEquals(4, arquivos.size());
        for (Path f : arquivos) {
            byte[] bytes = Files.readAllBytes(f);
            assertTrue(bytes.length > 1000, f + " está pequeno demais");
            // Assinatura PNG: 89 50 4E 47.
            assertEquals((byte) 0x89, bytes[0]);
            assertEquals((byte) 'P', bytes[1]);
            assertEquals((byte) 'N', bytes[2]);
            assertEquals((byte) 'G', bytes[3]);
        }
    }

    @Test
    void figurasSemCurvasCorrespondentesSaoOmitidas(@TempDir Path tmp) throws IOException {
        // Só BPSK em AWGN sem código: não há Hamming nem linhas de código.
        Path csv = tmp.resolve("so_awgn.csv");
        Files.writeString(csv, CABECALHO
                + "BPSK,AWGN,NENHUM,4.0,1,1000,20\n"
                + "BPSK,AWGN,NENHUM,4.0,2,1000,30\n"
                + "BPSK,AWGN,NENHUM,8.0,1,1000,2\n"
                + "BPSK,AWGN,NENHUM,8.0,2,1000,3\n", StandardCharsets.UTF_8);

        List<Path> gerados = PlotGenerator.gerarTodas(csv, tmp.resolve("figuras"));

        // Fig. 1, 2 e 3 existem (com BPSK); fig. 4 (linhas) é omitida.
        assertEquals(3, gerados.size());
        assertTrue(gerados.stream().noneMatch(p -> p.getFileName().toString().startsWith("fig4")));
    }

    private static String linhasCompletas() {
        StringBuilder sb = new StringBuilder();
        for (String esquema : new String[] {"BPSK", "AMI"}) {
            for (String canal : new String[] {"AWGN", "RAJADAS"}) {
                for (String codigo : new String[] {"NENHUM", "HAMMING"}) {
                    for (double ebn0 : new double[] {4.0, 8.0}) {
                        for (int semente = 1; semente <= 2; semente++) {
                            int erros = (ebn0 < 6 ? 100 : 10) + semente;
                            sb.append(String.format(Locale.ROOT, "%s,%s,%s,%.1f,%d,1000,%d\n",
                                    esquema, canal, codigo, ebn0, semente, erros));
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}