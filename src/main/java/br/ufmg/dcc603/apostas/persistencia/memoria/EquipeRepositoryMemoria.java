package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;

import java.util.Optional;

public final class EquipeRepositoryMemoria implements EquipeRepository {
    private final EstadoMemoria estado;

    public EquipeRepositoryMemoria(EstadoMemoria estado) {
        this.estado = estado;
    }

    @Override
    public void salvar(Equipe equipe) {
        estado.equipes().put(equipe.id(), equipe);
    }

    @Override
    public Optional<Equipe> buscarPorId(long id) {
        return Optional.ofNullable(estado.equipes().get(id));
    }

    @Override
    public Optional<Equipe> buscarPorNome(String nome) {
        return estado.equipes().values().stream()
                .filter(equipe -> equipe.nome().equalsIgnoreCase(nome.trim()))
                .findFirst();
    }
}
