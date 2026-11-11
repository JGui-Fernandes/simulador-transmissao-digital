package br.unisinos.edu.codificacao.canal;

public interface ChannelEncoder {
    int[] encode(int[] bits);

    double taxa();
}
