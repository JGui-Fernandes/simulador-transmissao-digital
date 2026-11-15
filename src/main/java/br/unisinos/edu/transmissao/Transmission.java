package br.unisinos.edu.transmissao;

import br.unisinos.edu.canal.AwgnChannel;
import br.unisinos.edu.canal.BurstChannel;
import br.unisinos.edu.canal.Channel;
import br.unisinos.edu.canal.SnrConverter;
import br.unisinos.edu.codificacao.canal.Hamming74;
import br.unisinos.edu.codificacao.linha.LineDecoder;
import br.unisinos.edu.codificacao.linha.LineEncoder;
import br.unisinos.edu.metricas.Metrics;
import br.unisinos.edu.modulacao.Complex;
import br.unisinos.edu.modulacao.Demodulator;
import br.unisinos.edu.modulacao.Modulator;

import java.util.Objects;
import java.util.Random;

/**
 * Recebe bits para servir à CLI e aos experimentos.
 */
public final class Transmission {

    /**
     * O Random do canal usa semente + este valor. Dados e ruído com a mesma
     * semente sairiam do mesmo fluxo pseudoaleatório e ficariam correlacionados.
     */
    public static final long DESLOCAMENTO_SEMENTE_CANAL = 1000L;

    private Transmission() {
    }

    public static TransmissionResult executar(int[] bits, ConfigTransmission config) {
        Objects.requireNonNull(bits, "bits não pode ser nulo");
        Objects.requireNonNull(config, "config não pode ser nula");
        if (bits.length == 0) {
            throw new IllegalArgumentException("Sequência vazia: nada para transmitir");
        }

        boolean linha = config.modo() == Mode.LINHA;
        Hamming74 hamming = new Hamming74();
        int[] transmitidos = config.hamming() ? hamming.encode(bits) : bits;
        double taxa = config.hamming() ? hamming.taxa() : 1.0;

        double sigmaMedio = linha
                ? SnrConverter.sigmaBandaBase(config.ebn0Db(),
                SnrConverter.energiaPorBitBandaBase(config.linha()), taxa)
                : SnrConverter.sigmaPassaBanda(SnrConverter.ebn0ParaEsn0Db(
                config.ebn0Db(), config.modulacao().bitsPorSimbolo(), taxa));

        Channel canal;
        double sigma;
        if (config.canal() == ChannelType.RAJADAS) {
            BurstChannel rajadas = linha
                    ? BurstChannel.bandaBase(config.p(), config.r(), config.fatorDegradacao())
                    : BurstChannel.passaBanda(config.p(), config.r(), config.fatorDegradacao());
            // Mesmo Eb/N0 médio do AWGN: o sigma do estado bom é reduzido
            sigma = rajadas.sigmaBomParaMesmaPotenciaMedia(sigmaMedio);
            canal = rajadas;
        } else {
            canal = linha ? AwgnChannel.bandaBase() : AwgnChannel.passaBanda();
            sigma = sigmaMedio;
        }

        Random rnd = new Random(config.semente() + DESLOCAMENTO_SEMENTE_CANAL);
        int[] detectados;
        if (linha) {
            Complex[] enviado = Complex.deReais(LineEncoder.codificar(transmitidos, config.linha()));
            Complex[] recebido = canal.apply(enviado, sigma, rnd);
            detectados = LineDecoder.decodificar(Complex.partesReais(recebido), config.linha());
        } else {
            Complex[] enviado = Modulator.modular(transmitidos, config.modulacao());
            Complex[] recebido = canal.apply(enviado, sigma, rnd);
            detectados = Demodulator.demodular(recebido, config.modulacao(), transmitidos.length);
        }

        int[] recebidos = config.hamming() ? hamming.decode(detectados, bits.length) : detectados;
        Metrics.ResultadoBer ber = Metrics.ber(bits, recebidos);

        double eficienciaBase = linha
                ? config.linha().eficienciaEspectral()
                : config.modulacao().eficienciaEspectral();

        return new TransmissionResult(config, bits, recebidos, ber,
                transmitidos.length, taxa, eficienciaBase * taxa, sigma);
    }
}