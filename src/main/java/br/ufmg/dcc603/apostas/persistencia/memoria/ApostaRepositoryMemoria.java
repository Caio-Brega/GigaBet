package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;

import java.util.Comparator;
import java.util.List;

public final class ApostaRepositoryMemoria implements ApostaRepository {
    private final EstadoMemoria estado;

    public ApostaRepositoryMemoria(EstadoMemoria estado) {
        this.estado = estado;
    }

    @Override
    public void salvar(Aposta aposta) {
        estado.apostas().put(aposta.id(), aposta);
    }

    @Override
    public List<Aposta> listar() {
        return ordenar(estado.apostas().values().stream().toList());
    }

    @Override
    public List<Aposta> listarPorApostador(long apostadorId) {
        return ordenar(estado.apostas().values().stream()
                .filter(aposta -> aposta.apostadorId() == apostadorId)
                .toList());
    }

    @Override
    public List<Aposta> listarPorPartida(long partidaId) {
        return ordenar(estado.apostas().values().stream()
                .filter(aposta -> aposta.partidaId() == partidaId)
                .toList());
    }

    @Override
    public int contarAbertas(long apostadorId) {
        return (int) estado.apostas().values().stream()
                .filter(aposta -> aposta.apostadorId() == apostadorId)
                .filter(aposta -> aposta.status() == StatusAposta.ABERTA)
                .count();
    }

    @Override
    public boolean existeAberta(long apostadorId, long partidaId) {
        return estado.apostas().values().stream()
                .anyMatch(aposta -> aposta.apostadorId() == apostadorId
                        && aposta.partidaId() == partidaId
                        && aposta.status() == StatusAposta.ABERTA);
    }

    private List<Aposta> ordenar(List<Aposta> apostas) {
        return apostas.stream().sorted(Comparator.comparing(Aposta::id)).toList();
    }
}
