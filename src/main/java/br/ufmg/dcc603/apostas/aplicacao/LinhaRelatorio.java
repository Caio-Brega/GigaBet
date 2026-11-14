package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;

public record LinhaRelatorio(
        long apostaId,
        long partidaId,
        String partida,
        String apostador,
        TipoPrevisao tipo,
        String previsao,
        BigDecimal valor,
        BigDecimal premioPotencial,
        StatusAposta status) {
}
