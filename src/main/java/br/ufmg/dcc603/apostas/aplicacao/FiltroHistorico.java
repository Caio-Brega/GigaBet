package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.StatusAposta;

import java.time.Instant;

public record FiltroHistorico(StatusAposta status, Instant de, Instant ate) {
    public static FiltroHistorico nenhum() {
        return new FiltroHistorico(null, null, null);
    }
}
