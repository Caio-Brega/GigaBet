package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.util.List;

public final class EstrategiaCartoesAmarelos implements EstrategiaProbabilidade {
    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.CARTOES_AMARELOS;
    }

    @Override
    public int espaco() {
        return 11;
    }

    @Override
    public int contar(PedidoAposta pedido, List<Partida> historico) {
        int ocorrencias = 0;
        for (Partida partida : historico) {
            if (partida.resultado().cartoesAgrupados() == pedido.cartoesAmarelos()) {
                ocorrencias++;
            }
        }
        return ocorrencias;
    }
}
