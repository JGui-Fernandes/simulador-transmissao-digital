package br.unisinos.edu;

import br.unisinos.edu.cli.Cli;

public class Main {

    public static void main(String[] args) {
        int codigo = Cli.executar(args, System.out, System.err);
        if (codigo != 0) {
            System.exit(codigo);
        }
    }
}