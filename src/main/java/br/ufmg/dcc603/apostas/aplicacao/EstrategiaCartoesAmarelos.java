package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class EstrategiaCartoesAmarelos implements EstrategiaProbabilidade {
    public static final int LIMITE_AGRUPAMENTO = 10;

    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.CARTOES_AMARELOS;
    }

    @Override
    public int espaco() {
        return 11;
    }

    @Override
    public BigDecimal calcularProbabilidade(Partida atual, PedidoAposta pedido, List<Partida> historico) {
        Integer k = pedido.cartoesAmarelos();
        if (k == null || k < 0 || k > LIMITE_AGRUPAMENTO) {
            return BigDecimal.ZERO;
        }

        EstatisticasPartida stats = EstatisticasPartida.calcular(atual, historico);
        double lambda = stats.lambdaCartoes();
        double p;
        if (k < LIMITE_AGRUPAMENTO) {
            p = DistribuicaoPoisson.probabilidade(lambda, k);
        } else {
            // Cauda acumulada P(X >= 10) = 1 - sum_{i=0}^9 P(X = i)
            double somaMenores = 0.0;
            for (int i = 0; i < LIMITE_AGRUPAMENTO; i++) {
                somaMenores += DistribuicaoPoisson.probabilidade(lambda, i);
            }
            p = Math.max(0.0, 1.0 - somaMenores);
        }
        return BigDecimal.valueOf(p).setScale(12, RoundingMode.HALF_UP);
    }
}
