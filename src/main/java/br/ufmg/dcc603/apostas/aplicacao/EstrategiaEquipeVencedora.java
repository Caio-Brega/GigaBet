package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.util.List;

public final class EstrategiaEquipeVencedora implements EstrategiaProbabilidade {
    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.EQUIPE_VENCEDORA;
    }

    @Override
    public int espaco() {
        return 3;
    }

    @Override
    public int contar(PedidoAposta pedido, List<Partida> historico) {
        int ocorrencias = 0;
        for (Partida partida : historico) {
            if (partida.resultado().desfecho() == pedido.desfecho()) {
                ocorrencias++;
            }
        }
        return ocorrencias;
    }
}
