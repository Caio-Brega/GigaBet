package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.aplicacao.Sessao;
import br.ufmg.dcc603.apostas.persistencia.SessaoRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SessaoRepositoryMemoria implements SessaoRepository {
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    @Override
    public void salvar(Sessao sessao) {
        sessoes.put(sessao.token(), sessao);
    }

    @Override
    public Optional<Sessao> buscar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessoes.get(token));
    }

    @Override
    public void remover(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }
}
