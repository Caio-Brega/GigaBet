package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.aplicacao.Sessao;

import java.util.Optional;

public interface SessaoRepository {
    void salvar(Sessao sessao);

    Optional<Sessao> buscar(String token);

    void remover(String token);
}
