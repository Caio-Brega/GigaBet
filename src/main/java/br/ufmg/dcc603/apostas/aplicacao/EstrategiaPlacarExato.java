package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.util.List;

public final class EstrategiaPlacarExato implements EstrategiaProbabilidade {
    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.PLACAR_EXATO;
    }

    @Override
    public int espaco() {
        return 100;
    }

    @Override
    public int contar(PedidoAposta pedido, List<Partida> historico) {
        int ocorrencias = 0;
        for (Partida partida : historico) {
            ResultadoPartida resultado = partida.resultado();
            if (resultado.golsMandante() == pedido.golsMandante()
                    && resultado.golsVisitante() == pedido.golsVisitante()) {
                ocorrencias++;
            }
        }
        return ocorrencias;
    }
}
