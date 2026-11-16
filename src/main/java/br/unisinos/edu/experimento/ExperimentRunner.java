package br.unisinos.edu.experimento;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ChannelType;
import br.unisinos.edu.transmissao.ConfigTransmission;
import br.unisinos.edu.transmissao.Mode;
import br.unisinos.edu.transmissao.Transmission;
import br.unisinos.edu.transmissao.TransmissionResult;
import br.unisinos.edu.mensagem.DataGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Executa o produto cartesiano esquema × canal × código × Eb/N0 × semente.
 * Os esquemas são a união explícita de modulações e códigos de linha; não há produto
 * cartesiano entre modo, linha e modulação, porque isso geraria combinações inválidas.
 */
public final class ExperimentRunner {

    private ExperimentRunner() {
    }

    /** Um esquema de transmissão: ou um código de linha, ou uma modulação. */
    record Esquema(Mode modo, LineCode linha, Modulation modulacao) {
        String nome() {
            return modo == Mode.LINHA ? linha.name() : modulacao.name();
        }
    }

    static List<Esquema> esquemas(ExperimentoConfig cfg) {
        List<Esquema> lista = new ArrayList<>();
        for (Modulation m : cfg.modulacoes()) {
            lista.add(new Esquema(Mode.MODULADO, null, m));
        }
        for (LineCode l : cfg.linhas()) {
            lista.add(new Esquema(Mode.LINHA, l, null));
        }
        return lista;
    }

    /** Número de execuções que {@link #executar} faz para esta configuração. */
    static int total(ExperimentoConfig cfg) {
        return esquemas(cfg).size()
                * cfg.canais().size()
                * cfg.codigosHamming().size()
                * cfg.ebn0Db().size()
                * cfg.sementes().size();
    }

    static ConfigTransmission montar(Esquema e, ChannelType canal, boolean hamming,
                                     double ebn0Db, long semente, ExperimentoConfig cfg) {
        ConfigTransmission base = e.modo() == Mode.LINHA
                ? ConfigTransmission.paraLinha(e.linha(), hamming, canal, ebn0Db, semente)
                : ConfigTransmission.paraModulacao(e.modulacao(), hamming, canal, ebn0Db, semente);
        return base.comRajadas(cfg.p(), cfg.r(), cfg.fator());
    }

    /**
     * Executa todas as combinações e grava cada resultado no CSV assim que sai.
     *
     * @param progresso recebe uma linha de andamento por execução
     */
    public static void executar(ExperimentoConfig cfg, Path saida, Consumer<String> progresso)
            throws IOException {
        Objects.requireNonNull(cfg, "cfg não pode ser nula");
        Objects.requireNonNull(saida, "saida não pode ser nula");
        Objects.requireNonNull(progresso, "progresso não pode ser nulo");

        List<Esquema> esquemas = esquemas(cfg);
        List<Long> sementes = cfg.sementes();
        int totalExecucoes = total(cfg);

        // Uma mensagem por semente, usada por todos os esquemas e SNRs (comparação pareada)
        int[][] mensagens = new int[sementes.size()][];
        for (int i = 0; i < sementes.size(); i++) {
            mensagens[i] = DataGenerator.bitsAleatorios(cfg.bits(), sementes.get(i));
        }

        int feitas = 0;
        Path temporario = saida.resolveSibling(saida.getFileName() + ".parcial");
        try (ResultWriter escritor = new ResultWriter(temporario)) {
            for (Esquema esquema : esquemas) {
                for (ChannelType canal : cfg.canais()) {
                    for (boolean hamming : cfg.codigosHamming()) {
                        for (double ebn0 : cfg.ebn0Db()) {
                            for (int i = 0; i < sementes.size(); i++) {
                                long semente = sementes.get(i);
                                ConfigTransmission config =
                                        montar(esquema, canal, hamming, ebn0, semente, cfg);
                                TransmissionResult resultado =
                                        Transmission.executar(mensagens[i], config);
                                escritor.escrever(resultado);
                                feitas++;
                                progresso.accept(String.format(Locale.ROOT,
                                        "[%d/%d] %s %s %s Eb/N0=%.1f dB semente=%d BER=%.3e",
                                        feitas, totalExecucoes, esquema.nome(), canal,
                                        hamming ? "HAMMING74" : "NENHUM",
                                        ebn0, semente, resultado.ber().taxa()));
                            }
                        }
                    }
                }
            }
        }
        Files.move(temporario, saida, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java -cp gb.jar br.unisinos.edu.experimento.ExperimentRunner "
                    + "<config.properties> <saida.csv>");
            System.exit(2);
        }

        ExperimentoConfig cfg;
        try {
            cfg = ConfigLoader.carregar(Path.of(args[0]));
        } catch (NoSuchFileException e) {
            System.err.println("Erro: arquivo de configuração não encontrado: " + e.getFile());
            System.exit(2);
            return;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Erro na configuração: " + e.getMessage());
            System.exit(2);
            return;
        }

        long inicio = System.nanoTime();
        try {
            executar(cfg, Path.of(args[1]), System.out::println);
        } catch (IOException e) {
            System.err.println("Erro ao gravar o CSV: " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.printf(Locale.ROOT, "Concluído: %d execuções em %.1f s -> %s%n",
                total(cfg), (System.nanoTime() - inicio) / 1e9, args[1]);
    }
}