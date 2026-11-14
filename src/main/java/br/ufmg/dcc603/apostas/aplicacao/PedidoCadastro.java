package br.ufmg.dcc603.apostas.aplicacao;

public record PedidoCadastro(String nome, String cpf, String email, String senha, boolean aceitouTermo) {
}
