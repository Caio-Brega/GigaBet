package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * premio = valor x max(oddsMinima, fatorMargem / p), com p = (n + 1) / (N + K).
 * O valor pago ao vencedor ja inclui a devolucao do valor apostado.
 */
public final class CalculadoraPremio {
    private final ParametrosCalculo parametros;

    public CalculadoraPremio(ParametrosCalculo parametros) {
        this.parametros = parametros;
    }

    public BigDecimal calcular(BigDecimal valor, Estimativa estimativa) {
        BigDecimal odds = parametros.fatorMargem()
                .multiply(BigDecimal.valueOf(estimativa.historico() + (long) estimativa.espaco()))
                .divide(BigDecimal.valueOf(estimativa.ocorrencias() + 1L), 12, RoundingMode.HALF_UP);
        if (odds.compareTo(parametros.oddsMinima()) < 0) {
            odds = parametros.oddsMinima();
        }
        return valor.multiply(odds).setScale(2, RoundingMode.HALF_UP);
    }
}
