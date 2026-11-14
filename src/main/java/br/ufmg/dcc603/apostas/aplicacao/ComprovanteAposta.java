package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.time.Instant;

public record ComprovanteAposta(
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
        BigDecimal saldoRestante) {
}
