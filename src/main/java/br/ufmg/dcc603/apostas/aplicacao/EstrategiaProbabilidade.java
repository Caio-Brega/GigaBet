package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.util.List;

public interface EstrategiaProbabilidade {
    TipoPrevisao tipo();

    int espaco();

    int contar(PedidoAposta pedido, List<Partida> historico);
}
