package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;

public record PedidoAposta(
        long partidaId,
        TipoPrevisao tipo,
        Desfecho desfecho,
        Integer golsMandante,
        Integer golsVisitante,
        Integer cartoesAmarelos,
        BigDecimal valor) {

    public static PedidoAposta vencedor(long partidaId, Desfecho desfecho, BigDecimal valor) {
        return new PedidoAposta(partidaId, TipoPrevisao.EQUIPE_VENCEDORA, desfecho, null, null, null, valor);
    }

    public static PedidoAposta placar(long partidaId, int golsMandante, int golsVisitante, BigDecimal valor) {
        return new PedidoAposta(partidaId, TipoPrevisao.PLACAR_EXATO, null, golsMandante, golsVisitante, null, valor);
    }

    public static PedidoAposta cartoes(long partidaId, int cartoesAmarelos, BigDecimal valor) {
        return new PedidoAposta(partidaId, TipoPrevisao.CARTOES_AMARELOS, null, null, null, cartoesAmarelos, valor);
    }
}
