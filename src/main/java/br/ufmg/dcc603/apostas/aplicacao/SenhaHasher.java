package br.ufmg.dcc603.apostas.aplicacao;

public interface SenhaHasher {
    String gerar(String senha);

    boolean confere(String senha, String hashArmazenado);
}
