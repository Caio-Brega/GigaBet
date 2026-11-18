package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CalculadoraProbabilidade {
    private final Map<TipoPrevisao, EstrategiaProbabilidade> estrategias = new EnumMap<>(TipoPrevisao.class);

    public CalculadoraProbabilidade(List<EstrategiaProbabilidade> estrategias) {
        for (EstrategiaProbabilidade estrategia : estrategias) {
            this.estrategias.put(estrategia.tipo(), estrategia);
        }
    }

    public static CalculadoraProbabilidade padrao() {
        return new CalculadoraProbabilidade(List.of(
                new EstrategiaEquipeVencedora(),
                new EstrategiaPlacarExato(),
                new EstrategiaCartoesAmarelos()));
    }

    public Estimativa estimar(Partida atual, PedidoAposta pedido, List<Partida> partidas) {
        EstrategiaProbabilidade estrategia = estrategias.get(pedido.tipo());
        if (estrategia == null) {
            throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA);
        }
        BigDecimal p = estrategia.calcularProbabilidade(atual, pedido, partidas);
        return new Estimativa(p, pedido.tipo(), estrategia.espaco());
    }
}
