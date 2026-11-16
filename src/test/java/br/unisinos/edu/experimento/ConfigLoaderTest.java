package br.unisinos.edu.experimento;

import br.unisinos.edu.modulacao.Modulation;
import br.unisinos.edu.transmissao.ConfigTransmission;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private static Properties propsValidas() {
        Properties p = new Properties();
        p.setProperty("ebn0.valores", "-2,0,2");
        p.setProperty("bits", "1000");
        p.setProperty("sementes", "1,2");
        p.setProperty("modulacoes", "BPSK, QAM16");
        p.setProperty("linhas", "AMI");
        p.setProperty("canais", "AWGN,RAJADAS");
        p.setProperty("codigos", "NENHUM,HAMMING74");
        return p;
    }

    @Test
    void leListasEAplicaValoresPadrao() {
        ExperimentoConfig cfg = ConfigLoader.parse(propsValidas());

        assertEquals(List.of(-2.0, 0.0, 2.0), cfg.ebn0Db());
        assertEquals(1000, cfg.bits());
        assertEquals(List.of(1L, 2L), cfg.sementes());
        assertEquals(List.of(Modulation.BPSK, Modulation.QAM16), cfg.modulacoes());
        assertEquals(List.of(false, true), cfg.codigosHamming());
        assertEquals(ConfigTransmission.P_PADRAO, cfg.p());
    }

    @Test
    void modulacaoDesconhecidaFalhaComMensagemClara() {
        Properties p = propsValidas();
        p.setProperty("modulacoes", "BPSK,8PSK");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("8PSK"));
    }

    @Test
    void chaveAusenteFalha() {
        Properties p = propsValidas();
        p.remove("bits");
        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.parse(p));
    }

    @Test
    void arquivoPadraoCorrespondeAoPlanoDaEtapa10() throws IOException {
        ExperimentoConfig cfg = ConfigLoader.carregar(Path.of("config", "experimento_base.properties"));

        assertEquals(8, cfg.ebn0Db().size());
        assertEquals(1_000_000, cfg.bits());
        assertEquals(5, cfg.sementes().size());
        assertEquals(5, ExperimentRunner.esquemas(cfg).size());
        assertEquals(800, ExperimentRunner.total(cfg));
    }

    @Test
    void itemVazioFalhaNomeandoAChave() {
        Properties p = propsValidas();
        p.setProperty("codigos", " , ");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("codigos"));
    }

    @Test
    void codigosVazioEhRejeitadoPeloExperimentoConfig() {
        Properties p = propsValidas();
        p.setProperty("codigos", "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("codigos"), e.getMessage());
    }

    @Test
    void permiteSomenteModulacao() {
        Properties p = propsValidas();
        p.setProperty("linhas", "");

        ExperimentoConfig cfg = ConfigLoader.parse(p);

        assertTrue(cfg.linhas().isEmpty());
        assertEquals(List.of(Modulation.BPSK, Modulation.QAM16), cfg.modulacoes());
    }

    @Test
    void rejeitaItemVazioNoMeioDaLista() {
        Properties p = propsValidas();
        p.setProperty("modulacoes", "BPSK,,QPSK");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("modulacoes"), e.getMessage());
    }

    @Test
    void rejeitaBitsForaDoIntervaloDeInt() {
        Properties p = propsValidas();
        p.setProperty("bits", "2147483648");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("bits"), e.getMessage());
    }

    @Test
    void rejeitaEbn0NaoFinito() {
        Properties p = propsValidas();
        p.setProperty("ebn0.valores", "NaN");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("ebn0.valores"), e.getMessage());
    }

    @Test
    void rejeitaSementesRepetidas() {
        Properties p = propsValidas();
        p.setProperty("sementes", "1,1");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.parse(p));
        assertTrue(e.getMessage().contains("sementes"), e.getMessage());
    }
}