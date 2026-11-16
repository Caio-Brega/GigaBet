package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.dominio.Equipe;

import java.util.List;
import java.util.Optional;

public interface EquipeRepository {
    void salvar(Equipe equipe);

    Optional<Equipe> buscarPorId(long id);

    Optional<Equipe> buscarPorNome(String nome);

    List<Equipe> listar();
}
