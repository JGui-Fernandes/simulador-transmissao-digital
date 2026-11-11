package br.unisinos.edu.codificacao.canal;

public interface ChannelDecoder {
    int[] decode(int[] coded, int originalLength);
}
