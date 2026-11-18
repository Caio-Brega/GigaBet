package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class PartidaRepositoryMemoria implements PartidaRepository {
    private final EstadoMemoria estado;

    public PartidaRepositoryMemoria(EstadoMemoria estado) {
        this.estado = estado;
    }

    @Override
    public void salvar(Partida partida) {
        estado.partidas().put(partida.id(), partida);
    }

    @Override
    public Optional<Partida> buscarPorId(long id) {
        return Optional.ofNullable(estado.partidas().get(id));
    }

    @Override
    public Optional<Partida> buscarPorExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        String limpo = externalId.trim();
        return estado.partidas().values().stream()
                .filter(p -> limpo.equals(p.externalId()))
                .findFirst();
    }

    @Override
    public List<Partida> listar() {
        return estado.partidas().values().stream()
                .sorted(Comparator.comparing(Partida::inicio).thenComparing(Partida::id))
                .toList();
    }
}
