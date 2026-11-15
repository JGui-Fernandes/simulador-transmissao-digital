package br.unisinos.edu.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CliTest {

    private static final Pattern POSICOES =
            Pattern.compile("Posições dos bits errados \\([^)]*\\): \\[(.*)\\]");

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

    /** Conteúdo entre colchetes da linha de posições dos bits errados. */
    private static String posicoes(String saida) {
        Matcher m = POSICOES.matcher(saida);
        assertTrue(m.find(), saida);
        return m.group(1);
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
        assertEquals("", posicoes(e.saida()));
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
    void modoLinhaComManchesterNosPadroes() {
        Execucao e = executar("--mensagem", "Redes", "--modo", "LINHA", "--ebn0", "30");

        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.saida().contains("Modo: LINHA ("), e.saida());
        assertTrue(e.saida().toUpperCase(Locale.ROOT).contains("MANCHESTER"), e.saida());
        assertTrue(e.saida().contains("Canal: AWGN"), e.saida());
        assertTrue(e.saida().contains("Código de canal: nenhum"), e.saida());
        assertTrue(e.saida().contains("Bits transmitidos no canal: 40"), e.saida());
        assertTrue(e.saida().contains("Mensagem recuperada: sim"), e.saida());
        assertEquals("", e.erro());
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
        assertTrue(erradosM.find(), e.saida());

        int errados = Integer.parseInt(erradosM.group(1));
        String lista = posicoes(e.saida()).trim();
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

    @Test
    void aceitaEbn0NegativoComoValor() {
        Execucao e = executar("--mensagem", "Redes", "--ebn0", "-2", "--semente", "1");
        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.saida().contains("Eb/N0: -2.00 dB"), e.saida());
    }

    // ---------- Opções que não se aplicam ----------

    @Test
    void opcaoNaoAplicavelEhIgnoradaComAvisoEmErr() {
        Execucao e = executar("--mensagem", "Redes", "--modo", "LINHA", "--linha", "AMI",
                "--modulacao", "QAM16", "--ebn0", "30");

        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.erro().contains("Aviso: a opção --modulacao foi ignorada"), e.erro());
        assertFalse(e.erro().contains("Erro"), e.erro());
        assertTrue(e.saida().contains("Modo: LINHA (AMI)"), e.saida());
        assertTrue(e.saida().contains("Mensagem recuperada: sim"), e.saida());
        assertFalse(e.saida().contains("Aviso"), e.saida());
    }

    @Test
    void parametroDeRajadasComAwgnGeraAvisoEMantemASaida() {
        Execucao e = executar("--mensagem", "Redes", "--p", "0.02", "--ebn0", "30");

        assertEquals(0, e.codigo(), e.erro());
        assertTrue(e.erro().contains("--p foi ignorada"), e.erro());
        assertFalse(e.erro().contains("--r"), e.erro());
        assertTrue(e.saida().contains("Canal: AWGN"), e.saida());
    }

    @Test
    void valorInvalidoEhRejeitadoMesmoQuandoAOpcaoNaoSeAplica() {
        Execucao e = executar("--modo", "LINHA", "--modulacao", "PSK8");
        assertEquals(2, e.codigo());
        assertTrue(e.erro().contains("--modulacao"), e.erro());
        assertEquals("", e.saida());
    }

    // ---------- Ajuda e erros de uso ----------

    @Test
    void helpMostraAsOpcoesESaiComSucesso() {
        Execucao e = executar("--help");
        assertEquals(0, e.codigo());
        assertTrue(e.saida().contains("--mensagem"), e.saida());
        assertTrue(e.saida().contains("--ebn0"), e.saida());
        assertTrue(e.saida().contains("--canal"), e.saida());
        assertEquals("", e.erro());
    }

    @Test
    void ajudaAntigoNaoEhMaisAceito() {
        Execucao e = executar("--ajuda");
        assertEquals(2, e.codigo());
        assertTrue(e.erro().contains("opção desconhecida: --ajuda"), e.erro());
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
        Execucao ausente = executar("--ebn0");
        assertEquals(2, ausente.codigo());
        assertTrue(ausente.erro().contains("falta o valor de --ebn0"), ausente.erro());

        assertEquals(2, executar("Redes").codigo());
        assertEquals(2, executar("--ebn0", "1", "--ebn0", "2").codigo());
    }

    @Test
    void valorComecandoComDoisHifensEhRejeitado() {
        Execucao e = executar("--mensagem", "--modo");
        assertEquals(2, e.codigo());
        assertTrue(e.erro().contains("falta o valor de --mensagem"), e.erro());
        assertEquals("", e.saida());
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
        assertTrue(e.erro().contains("--help"), e.erro());
    }

    // ---------- Texto seguro ----------

    @Test
    void textoSeguroEscapaCaracteresNaoImprimiveis() {
        assertEquals("Redes", Cli.textoSeguro("Redes"));
        assertEquals("a\\x00b\\x7Fc\\xC3", Cli.textoSeguro("a\u0000b\u007Fc\u00C3"));
        assertEquals("\\x0A", Cli.textoSeguro("\n"));
    }
}