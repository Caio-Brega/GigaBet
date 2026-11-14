package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Perfil;

public record Sessao(String token, long usuarioId, Perfil perfil) {
}
