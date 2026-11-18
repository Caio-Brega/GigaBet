package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.dominio.Partida;

import java.util.List;
import java.util.Optional;

public interface PartidaRepository {
    void salvar(Partida partida);

    Optional<Partida> buscarPorId(long id);

    Optional<Partida> buscarPorExternalId(String externalId);

    List<Partida> listar();
}
