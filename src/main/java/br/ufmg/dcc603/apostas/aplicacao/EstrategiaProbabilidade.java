package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.util.List;

public interface EstrategiaProbabilidade {
    TipoPrevisao tipo();

    int espaco();

    BigDecimal calcularProbabilidade(Partida atual, PedidoAposta pedido, List<Partida> historico);

    default int contar(PedidoAposta pedido, List<Partida> historico) {
        return 0;
    }
}
