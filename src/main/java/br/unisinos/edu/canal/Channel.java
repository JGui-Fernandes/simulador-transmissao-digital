package br.unisinos.edu.canal;

import br.unisinos.edu.modulacao.Complex;

import java.util.Random;

public interface Channel {

    /**
     * @param sinal sinal transmitido
     * @param sigma desvio padrão do ruído por componente
     * @param rnd   gerador aleatório, com semente controlada pelo experimento
     * @return novo array com o sinal recebido
     */
    Complex[] apply(Complex[] sinal, double sigma, Random rnd);
}