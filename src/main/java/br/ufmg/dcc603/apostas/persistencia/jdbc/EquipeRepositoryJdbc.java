package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;

import java.util.List;
import java.util.Optional;

public final class EquipeRepositoryJdbc implements EquipeRepository {
    private final Banco banco;

    public EquipeRepositoryJdbc(Banco banco) {
        this.banco = banco;
    }

    @Override
    public void salvar(Equipe equipe) {
        banco.atualizar("""
                INSERT INTO equipes (id, nome) VALUES (?, ?)
                ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome
                """, equipe.id(), equipe.nome());
    }

    @Override
    public Optional<Equipe> buscarPorId(long id) {
        return banco.consultarUm(
                "SELECT id, nome FROM equipes WHERE id = ?",
                this::mapear, id);
    }

    @Override
    public Optional<Equipe> buscarPorNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        return banco.consultarUm(
                "SELECT id, nome FROM equipes WHERE lower(nome) = lower(?)",
                this::mapear, nome.trim());
    }

    @Override
    public List<Equipe> listar() {
        return banco.listar("SELECT id, nome FROM equipes ORDER BY lower(nome), id", this::mapear);
    }

    private Equipe mapear(java.sql.ResultSet linhas) throws java.sql.SQLException {
        return new Equipe(linhas.getLong("id"), linhas.getString("nome"));
    }
}
