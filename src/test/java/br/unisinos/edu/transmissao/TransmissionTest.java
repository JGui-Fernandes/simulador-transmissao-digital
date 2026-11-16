package br.unisinos.edu.transmissao;

import br.unisinos.edu.codificacao.linha.LineCode;
import br.unisinos.edu.mensagem.DataGenerator;
import br.unisinos.edu.modulacao.Modulation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransmissionTest {

    private static final String TEXTO = "Redes de Computadores - GB 2026!";

    /** Todas as combinações: (linha ou modulação) × (com ou sem Hamming) × canal. */
    private static List<ConfigTransmission> todasAsCombinacoes(double ebn0Db, long semente) {
        List<ConfigTransmission> lista = new ArrayList<>();
        for (boolean hamming : new boolean[] {false, true}) {
            for (ChannelType canal : ChannelType.values()) {
                for (LineCode l : LineCode.values()) {
                    lista.add(ConfigTransmission.paraLinha(l, hamming, canal, ebn0Db, semente));
                }
                for (Modulation m : Modulation.values()) {
                    lista.add(ConfigTransmission.paraModulacao(m, hamming, canal, ebn0Db, semente));
                }
            }
        }
        return lista;
    }

    // ---------- Consistência ----------

    @Test
    void emEbN0AltoATodasAsCombinacoesRecuperamAMensagem() {
        int[] bits = DataGenerator.textoParaBits(TEXTO);
        List<ConfigTransmission> combinacoes = todasAsCombinacoes(30.0, 42L);
        assertEquals(20, combinacoes.size());   // 2 códigos × 2 canais × (2 linhas + 3 modulações)

        for (ConfigTransmission c : combinacoes) {
            TransmissionResult r = Transmission.executar(bits, c);
            assertEquals(TEXTO, DataGenerator.bitsParaTexto(r.bitsRecebidos()), c.toString());
            assertEquals(0, r.ber().bitsErrados(), c.toString());
            assertTrue(r.mensagemRecuperada(), c.toString());
        }
    }

    @Test
    void ebN0AltoRecuperaAMensagemParaVariasSementes() {
        int[] bits = DataGenerator.textoParaBits(TEXTO);
        for (long semente = 1; semente <= 5; semente++) {
            for (ConfigTransmission c : todasAsCombinacoes(30.0, semente)) {
                assertTrue(Transmission.executar(bits, c).mensagemRecuperada(), c.toString());
            }
        }
    }

    // ---------- Posições dos bits errados ----------

    @Test
    void comRuidoModeradoAsPosicoesInformadasCoincidemComAsDiferencas() {
        int[] bits = DataGenerator.bitsAleatorios(10_000, 7L);
        for (ConfigTransmission c : List.of(
                ConfigTransmission.paraModulacao(Modulation.QPSK, false, ChannelType.AWGN, 2.0, 7L),
                ConfigTransmission.paraModulacao(Modulation.QAM16, true, ChannelType.AWGN, 4.0, 7L),
                ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, 4.0, 7L),
                ConfigTransmission.paraLinha(LineCode.MANCHESTER, true, ChannelType.RAJADAS, 2.0, 7L))) {
            TransmissionResult r = Transmission.executar(bits, c);

            List<Integer> esperadas = new ArrayList<>();
            for (int i = 0; i < bits.length; i++) {
                if (bits[i] != r.bitsRecebidos()[i]) {
                    esperadas.add(i);
                }
            }
            assertFalse(esperadas.isEmpty(), "o ruído deveria causar erros: " + c);
            assertEquals(esperadas, r.ber().posicoesErradas(), c.toString());
            assertEquals(esperadas.size(), r.ber().bitsErrados(), c.toString());
            assertEquals(bits.length, r.ber().totalBits());
            assertFalse(r.mensagemRecuperada());
        }
    }

    @Test
    void naoAlteraOsBitsDeEntrada() {
        int[] bits = DataGenerator.bitsAleatorios(1_000, 3L);
        int[] copia = bits.clone();
        Transmission.executar(bits, ConfigTransmission.paraModulacao(Modulation.QPSK, true, ChannelType.AWGN, 0.0, 3L));
        assertArrayEquals(copia, bits);
    }

    // ---------- Tamanhos e taxa do código ----------

    @Test
    void hammingComPaddingDevolveOTamanhoOriginal() {
        // 1001 bits: 251 blocos de 4 (3 zeros de padding) -> 1757 bits no canal;
        // 1757 não é múltiplo de 4, então o 16-QAM ainda acrescenta 3 zeros no último símbolo
        int[] bits = DataGenerator.bitsAleatorios(1_001, 5L);
        TransmissionResult r = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.QAM16, true, ChannelType.AWGN, 30.0, 5L));

        assertEquals(1_757, r.bitsTransmitidos());
        assertEquals(1_001, r.bitsRecebidos().length);
        assertArrayEquals(bits, r.bitsRecebidos());
        assertEquals(4.0 / 7.0, r.taxaCodigo(), 1e-12);
    }

    @Test
    void semCodigoDeCanalTaxaEUmETransmitidosIgualAoTamanho() {
        int[] bits = DataGenerator.bitsAleatorios(100, 5L);
        TransmissionResult r = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, 30.0, 5L));
        assertEquals(1.0, r.taxaCodigo(), 1e-12);
        assertEquals(100, r.bitsTransmitidos());
    }

    // ---------- Eficiência espectral ----------

    @Test
    void eficienciaEspectralNominalMultiplicadaPelaTaxaDoCodigo() {
        int[] bits = DataGenerator.textoParaBits(TEXTO);
        double[] semCodigo = {1.0, 2.0, 4.0};
        Modulation[] mods = {Modulation.BPSK, Modulation.QPSK, Modulation.QAM16};
        for (int i = 0; i < mods.length; i++) {
            assertEquals(semCodigo[i], Transmission.executar(bits,
                            ConfigTransmission.paraModulacao(mods[i], false, ChannelType.AWGN, 30.0, 1L))
                    .eficienciaEspectral(), 1e-12, mods[i].rotulo());
            assertEquals(semCodigo[i] * 4.0 / 7.0, Transmission.executar(bits,
                            ConfigTransmission.paraModulacao(mods[i], true, ChannelType.AWGN, 30.0, 1L))
                    .eficienciaEspectral(), 1e-12, mods[i].rotulo() + " com Hamming");
        }
        assertEquals(1.0, Transmission.executar(bits,
                        ConfigTransmission.paraLinha(LineCode.AMI, false, ChannelType.AWGN, 30.0, 1L))
                .eficienciaEspectral(), 1e-12);
        assertEquals(0.5, Transmission.executar(bits,
                        ConfigTransmission.paraLinha(LineCode.MANCHESTER, false, ChannelType.AWGN, 30.0, 1L))
                .eficienciaEspectral(), 1e-12);
    }

    // ---------- Reprodutibilidade ----------

    @Test
    void mesmaSementeRepeteOResultadoInclusiveNasRajadas() {
        // o canal de rajadas guarda estado: duas execuções seguidas só repetem
        // o resultado se cada uma criar o seu próprio canal
        int[] bits = DataGenerator.bitsAleatorios(50_000, 11L);
        for (ChannelType canal : ChannelType.values()) {
            ConfigTransmission c = ConfigTransmission.paraModulacao(Modulation.QPSK, false, canal, 3.0, 11L);
            TransmissionResult a = Transmission.executar(bits, c);
            TransmissionResult b = Transmission.executar(bits, c);
            assertArrayEquals(a.bitsRecebidos(), b.bitsRecebidos(), canal.toString());
            assertEquals(a.ber().posicoesErradas(), b.ber().posicoesErradas(), canal.toString());
        }
    }

    @Test
    void sementesDiferentesGeramRuidosDiferentes() {
        int[] bits = DataGenerator.bitsAleatorios(50_000, 11L);
        TransmissionResult a = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.QPSK, false, ChannelType.AWGN, 3.0, 1L));
        TransmissionResult b = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.QPSK, false, ChannelType.AWGN, 3.0, 2L));
        assertNotEquals(a.ber().posicoesErradas(), b.ber().posicoesErradas());
    }

    // ---------- Comportamento dos canais ----------

    @Test
    void sigmaDoAwgnSegueOSnrConverter() {
        int[] bits = DataGenerator.bitsAleatorios(100, 1L);
        // BPSK, R = 1: Es/N0 = Eb/N0 = 0 dB -> sigma = sqrt(1/2)
        TransmissionResult r = Transmission.executar(bits,
                ConfigTransmission.paraModulacao(Modulation.BPSK, false, ChannelType.AWGN, 0.0, 1L));
        assertEquals(Math.sqrt(0.5), r.sigma(), 1e-12);
    }

    @Test
    void rajadasUsamOMesmoSigmaNominalDoAwgn() {
        int[] bits = DataGenerator.bitsAleatorios(100, 1L);
        double awgn = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.MANCHESTER, false, ChannelType.AWGN, 4.0, 1L)).sigma();
        double rajadas = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.MANCHESTER, false, ChannelType.RAJADAS, 4.0, 1L)).sigma();
        assertEquals(awgn, rajadas, 1e-12,
                "o sigma reportado deve ser o nominal, igual ao do AWGN: " + rajadas + " x " + awgn);
    }

    // Fator 5 aqui é deliberado: é o cenário de validação do modelo, com agrupamento
// bem visível. O experimento final usa fator 2 (ver docs/parametros.md).
    private static final double FATOR_VALIDACAO_MODELO = 5.0;

    @Test
    void noMesmoEbN0MedioRajadasTemBerMaiorQueAwgnEErrosAgrupados() {
        int[] bits = DataGenerator.bitsAleatorios(1_000_000, 2026L);
        TransmissionResult awgn = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.MANCHESTER, false, ChannelType.AWGN, 4.0, 2026L));
        TransmissionResult rajadas = Transmission.executar(bits,
                ConfigTransmission.paraLinha(LineCode.MANCHESTER, false, ChannelType.RAJADAS, 4.0, 2026L)
                        .comRajadas(0.01, 0.1, FATOR_VALIDACAO_MODELO));

        double berAwgn = awgn.ber().taxa();
        double berRajadas = rajadas.ber().taxa();
        assertEquals(0.0125, berAwgn, 0.0125 * 0.10);   // Q(√(2·Eb/N0)) em 4 dB
        assertTrue(berRajadas > 1.3 * berAwgn, "rajadas: " + berRajadas + ", AWGN: " + berAwgn);

        // erros agrupados: P(dois seguidos) bem acima de BER² (valor se fossem independentes)
        List<Integer> pos = rajadas.ber().posicoesErradas();
        int consecutivos = 0;
        for (int i = 0; i + 1 < pos.size(); i++) {
            if (pos.get(i + 1) == pos.get(i) + 1) {
                consecutivos++;
            }
        }
        double taxaConsecutivos = (double) consecutivos / (bits.length - 1);
        assertTrue(taxaConsecutivos > 3 * berRajadas * berRajadas,
                "P(consecutivos) = " + taxaConsecutivos + ", BER² = " + berRajadas * berRajadas);
    }

    // ---------- Validação ----------

    @Test
    void rejeitaEntradasInvalidas() {
        ConfigTransmission c = ConfigTransmission.paraModulacao(Modulation.BPSK, false, ChannelType.AWGN, 4.0, 1L);
        assertThrows(NullPointerException.class, () -> Transmission.executar(null, c));
        assertThrows(NullPointerException.class, () -> Transmission.executar(new int[] {1, 0}, null));
        assertThrows(IllegalArgumentException.class, () -> Transmission.executar(new int[0], c));
        assertThrows(IllegalArgumentException.class, () -> Transmission.executar(new int[] {0, 2}, c));
        assertThrows(IllegalArgumentException.class, () -> Transmission.executar(new int[] {0, 2},
                ConfigTransmission.paraLinha(LineCode.AMI, true, ChannelType.AWGN, 4.0, 1L)));
    }
}