package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.aplicacao.Sessao;
import br.ufmg.dcc603.apostas.dominio.Perfil;
import br.ufmg.dcc603.apostas.persistencia.SessaoRepository;

import java.util.Optional;

public final class SessaoRepositoryJdbc implements SessaoRepository {
    private final Banco banco;

    public SessaoRepositoryJdbc(Banco banco) {
        this.banco = banco;
    }

    @Override
    public void salvar(Sessao sessao) {
        banco.atualizar("""
                INSERT INTO sessoes (token, usuario_id, perfil) VALUES (?, ?, ?)
                ON CONFLICT (token) DO UPDATE SET
                    usuario_id = EXCLUDED.usuario_id,
                    perfil = EXCLUDED.perfil
                """, sessao.token(), sessao.usuarioId(), sessao.perfil());
    }

    @Override
    public Optional<Sessao> buscar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return banco.consultarUm(
                "SELECT token, usuario_id, perfil FROM sessoes WHERE token = ?",
                linhas -> new Sessao(
                        linhas.getString("token"),
                        linhas.getLong("usuario_id"),
                        Perfil.valueOf(linhas.getString("perfil"))),
                token);
    }

    @Override
    public void remover(String token) {
        if (token != null) {
            banco.atualizar("DELETE FROM sessoes WHERE token = ?", token);
        }
    }
}
