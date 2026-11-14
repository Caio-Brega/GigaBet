package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.dominio.Aposta;

import java.util.List;

public interface ApostaRepository {
    void salvar(Aposta aposta);

    List<Aposta> listar();

    List<Aposta> listarPorApostador(long apostadorId);

    List<Aposta> listarPorPartida(long partidaId);

    int contarAbertas(long apostadorId);

    boolean existeAberta(long apostadorId, long partidaId);
}
