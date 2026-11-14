package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemHistorico(
        long apostaId,
        long partidaId,
        String partida,
        TipoPrevisao tipo,
        String previsao,
        BigDecimal valor,
        BigDecimal probabilidade,
        BigDecimal premioPotencial,
        StatusAposta status,
        Instant criadaEm,
        ResultadoPartida resultado) {
}
