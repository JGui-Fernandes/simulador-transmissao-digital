package br.unisinos.edu.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CliTest {

    private record Execucao(int codigo, String saida, String erro) {
    }

    private static Execucao executar(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int codigo = Cli.executar(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Execucao(codigo, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    // ---------- Execução normal ----------

    @Test
    void exemploDoPlanoRecuperaAMensagem() {
        Execucao e = executar("--mensagem", "Redes", "--modo", "MODULADO", "--modulacao", "QAM16",
                "--ebn0", "30", "--canal", "AWGN", "--semente", "42");

        assertEquals(0, e.codigo());
        assertTrue(e.saida().contains("Modo: MODULADO (16-QAM)"), e.saida());
        assertTrue(e.saida().contains("Mensagem enviada: Redes"), e.saida());
        assertTrue(e.saida().contains("Mensagem recebida: Redes"), e.saida());
        assertTrue(e.saida().contains("Bits errados: 0"), e.saida());
        assertTrue(e.saida().contains("Posições dos bits errados: []"), e.saida());
        assertTrue(e.saida().contains("Eficiência espectral nominal: 4.00 bits/s/Hz"), e.saida());
        assertEquals("", e.erro());
    }

    @Test
    void semArgumentosUsaOsValoresPadrao() {
        Execucao e = executar();
        assertEquals(0, e.codigo());
        assertTrue(e.saida().contains("Modo: MODULADO (QPSK)"), e.saida());
        assertTrue(e.saida().contains("Canal: AWGN"), e.saida());
        assertTrue(e.saida().contains("Código de canal: nenhum"), e.saida());
        assertTrue(e.saida().contains("Eb/N0: 10.00 dB"), e.saida());
        assertTrue(e.saida().contains("Semente: 42"), e.saida());
    }

    @Test
    void modoLinhaComHammingERajadas() {
        Execucao e = executar("--mensagem", "Redes", "--modo", "linha", "--linha", "ami",
                "--codigo", "hamming74", "--canal", "rajadas", "--ebn0", "30");

        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.saida().contains("Modo: LINHA (AMI)"), e.saida());
        assertTrue(e.saida().contains("Código de canal: Hamming(7,4), R = 0.5714"), e.saida());
        assertTrue(e.saida().contains("Canal: rajadas (p=0.01, r=0.1, fator=5.0)"), e.saida());
        assertTrue(e.saida().contains("Bits transmitidos no canal: 70"), e.saida());   // 40 bits -> 10 blocos × 7
        assertTrue(e.saida().contains("Mensagem recuperada: sim"), e.saida());
        assertTrue(e.saida().contains("Eficiência espectral nominal: 0.57 bits/s/Hz"), e.saida());
    }

    @Test
    void aceitaParametrosDasRajadasEAliasDe16Qam() {
        Execucao e = executar("--mensagem", "Redes", "--modulacao", "16-QAM", "--canal", "RAJADAS",
                "--p", "0.02", "--r", "0,2", "--fator", "4", "--ebn0", "30");
        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.saida().contains("Modo: MODULADO (16-QAM)"), e.saida());
        assertTrue(e.saida().contains("Canal: rajadas (p=0.02, r=0.2, fator=4.0)"), e.saida());
    }

    @Test
    void comRuidoModeradoAsPosicoesBatemComONumeroDeBitsErrados() {
        Execucao e = executar("--mensagem", "Redes de Computadores - GB 2026!", "--modulacao", "QPSK",
                "--ebn0", "0", "--semente", "3");
        assertEquals(0, e.codigo(), e.erro());

        Matcher erradosM = Pattern.compile("Bits errados: (\\d+)").matcher(e.saida());
        Matcher posicoesM = Pattern.compile("Posições dos bits errados: \\[(.*)\\]").matcher(e.saida());
        assertTrue(erradosM.find() && posicoesM.find(), e.saida());

        int errados = Integer.parseInt(erradosM.group(1));
        String lista = posicoesM.group(1).trim();
        int quantasPosicoes = lista.isEmpty() ? 0 : lista.split(",").length;
        assertTrue(errados > 0, "0 dB deveria gerar erros: " + e.saida());
        assertEquals(errados, quantasPosicoes);
        assertTrue(e.saida().contains("Mensagem recuperada: não"), e.saida());
    }

    @Test
    void mesmaSementeRepeteASaida() {
        String[] args = {"--mensagem", "Redes de Computadores", "--ebn0", "0", "--semente", "9"};
        assertEquals(executar(args).saida(), executar(args).saida());
    }

    // ---------- Ajuda e erros de uso ----------

    @Test
    void ajudaMostraAsOpcoesESaiComSucesso() {
        Execucao e = executar("--ajuda");
        assertEquals(0, e.codigo());
        assertTrue(e.saida().contains("--mensagem"), e.saida());
        assertTrue(e.saida().contains("--ebn0"), e.saida());
        assertTrue(e.saida().contains("--canal"), e.saida());
    }

    @Test
    void rejeitaOpcaoDesconhecida() {
        Execucao e = executar("--foo", "1");
        assertEquals(2, e.codigo());
        assertTrue(e.erro().contains("opção desconhecida: --foo"), e.erro());
        assertEquals("", e.saida());
    }

    @Test
    void rejeitaValorAusenteArgumentoSoltoERepetido() {
        assertEquals(2, executar("--ebn0").codigo());
        assertTrue(executar("--ebn0").erro().contains("falta o valor"));
        assertEquals(2, executar("Redes").codigo());
        assertEquals(2, executar("--ebn0", "1", "--ebn0", "2").codigo());
    }

    @Test
    void rejeitaValoresInvalidos() {
        assertEquals(2, executar("--modo", "XYZ").codigo());
        assertEquals(2, executar("--modulacao", "PSK8").codigo());
        assertEquals(2, executar("--codigo", "CRC").codigo());
        assertEquals(2, executar("--canal", "RAYLEIGH").codigo());
        assertEquals(2, executar("--modo", "LINHA", "--linha", "NRZ").codigo());
        assertEquals(2, executar("--ebn0", "abc").codigo());
        assertEquals(2, executar("--ebn0", "NaN").codigo());
        assertEquals(2, executar("--semente", "1.5").codigo());
        assertEquals(2, executar("--p", "0").codigo());
        assertEquals(2, executar("--fator", "0.5").codigo());
        assertEquals(2, executar("--mensagem", "").codigo());
    }

    @Test
    void mensagemForaDoAsciiGeraErroAmigavelEmVezDeExcecao() {
        Execucao e = executar("--mensagem", "a\u00e7\u00e3o");
        assertEquals(2, e.codigo());
        assertTrue(e.erro().contains("Caractere fora do ASCII"), e.erro());
        assertTrue(e.erro().contains("--ajuda"), e.erro());
    }

    // ---------- Texto seguro ----------

    @Test
    void textoSeguroEscapaCaracteresNaoImprimiveis() {
        assertEquals("Redes", Cli.textoSeguro("Redes"));
        assertEquals("a\\x00b\\x7Fc\\xC3", Cli.textoSeguro("a\u0000b\u007Fc\u00C3"));
        assertEquals("\\x0A", Cli.textoSeguro("\n"));
    }
}