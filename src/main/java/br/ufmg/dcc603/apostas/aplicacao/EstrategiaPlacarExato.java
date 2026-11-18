package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class EstrategiaPlacarExato implements EstrategiaProbabilidade {
    public static final int MAX_GOLS = 9;

    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.PLACAR_EXATO;
    }

    @Override
    public int espaco() {
        return 100;
    }

    @Override
    public BigDecimal calcularProbabilidade(Partida atual, PedidoAposta pedido, List<Partida> historico) {
        Integer gx = pedido.golsMandante();
        Integer gy = pedido.golsVisitante();
        if (gx == null || gy == null || gx < 0 || gx > MAX_GOLS || gy < 0 || gy > MAX_GOLS) {
            return BigDecimal.ZERO;
        }

        EstatisticasPartida stats = EstatisticasPartida.calcular(atual, historico);
        double[][] matriz = calcularMatrizNormalizada(stats.lambdaMandante(), stats.lambdaVisitante());
        double p = matriz[gx][gy];
        return BigDecimal.valueOf(p).setScale(12, RoundingMode.HALF_UP);
    }

    /**
     * Calcula matriz 10x10 normalizada para que a soma total seja exatamente 1.0.
     */
    public static double[][] calcularMatrizNormalizada(double lambdaM, double lambdaV) {
        double[][] matriz = new double[MAX_GOLS + 1][MAX_GOLS + 1];
        double soma = 0.0;
        for (int x = 0; x <= MAX_GOLS; x++) {
            double px = DistribuicaoPoisson.probabilidade(lambdaM, x);
            for (int y = 0; y <= MAX_GOLS; y++) {
                double py = DistribuicaoPoisson.probabilidade(lambdaV, y);
                double pRaw = px * py;
                matriz[x][y] = pRaw;
                soma += pRaw;
            }
        }
        if (soma > 0.0) {
            for (int x = 0; x <= MAX_GOLS; x++) {
                for (int y = 0; y <= MAX_GOLS; y++) {
                    matriz[x][y] /= soma;
                }
            }
        }
        return matriz;
    }
}
