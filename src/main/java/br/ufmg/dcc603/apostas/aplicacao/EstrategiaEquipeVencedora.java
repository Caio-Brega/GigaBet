package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public BigDecimal calcularProbabilidade(Partida atual, PedidoAposta pedido, List<Partida> historico) {
        Desfecho desfecho = pedido.desfecho();
        if (desfecho == null) {
            return BigDecimal.ZERO;
        }

        EstatisticasPartida stats = EstatisticasPartida.calcular(atual, historico);
        double[][] matriz = EstrategiaPlacarExato.calcularMatrizNormalizada(
                stats.lambdaMandante(), stats.lambdaVisitante());

        double pMandante = 0.0;
        double pEmpate = 0.0;
        double pVisitante = 0.0;

        for (int x = 0; x <= EstrategiaPlacarExato.MAX_GOLS; x++) {
            for (int y = 0; y <= EstrategiaPlacarExato.MAX_GOLS; y++) {
                if (x > y) {
                    pMandante += matriz[x][y];
                } else if (x == y) {
                    pEmpate += matriz[x][y];
                } else {
                    pVisitante += matriz[x][y];
                }
            }
        }

        double p = switch (desfecho) {
            case MANDANTE -> pMandante;
            case EMPATE -> pEmpate;
            case VISITANTE -> pVisitante;
        };

        return BigDecimal.valueOf(p).setScale(12, RoundingMode.HALF_UP);
    }
}
