package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculadora de cotacoes (odds) e premio potencial.
 * odd = clamp(fatorMargem / p, min=oddsMinima, max=MAX_ODD_MERCADO)
 * premio = round(min(valor * odd, MAX_PAYOUT), 2)
 */
public final class CalculadoraPremio {
    private final ParametrosCalculo parametros;

    public CalculadoraPremio(ParametrosCalculo parametros) {
        this.parametros = parametros;
    }

    public BigDecimal calcular(BigDecimal valor, Estimativa estimativa) {
        return calcular(valor, estimativa.probabilidade(), estimativa.tipo());
    }

    public BigDecimal calcular(BigDecimal valor, BigDecimal probabilidade, TipoPrevisao tipo) {
        if (valor == null || probabilidade == null || probabilidade.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal odd = calcularOdd(probabilidade, tipo);
        BigDecimal premio = valor.multiply(odd).setScale(2, RoundingMode.HALF_UP);
        if (parametros.maxPayout() != null && premio.compareTo(parametros.maxPayout()) > 0) {
            premio = parametros.maxPayout().setScale(2, RoundingMode.HALF_UP);
        }
        return premio;
    }

    public BigDecimal calcularOdd(BigDecimal probabilidade, TipoPrevisao tipo) {
        if (probabilidade == null || probabilidade.compareTo(BigDecimal.ZERO) <= 0) {
            return parametros.maxOdd(tipo);
        }
        BigDecimal odd = parametros.fatorMargem().divide(probabilidade, 12, RoundingMode.HALF_UP);
        if (odd.compareTo(parametros.oddsMinima()) < 0) {
            odd = parametros.oddsMinima();
        }
        BigDecimal maxOdd = parametros.maxOdd(tipo);
        if (maxOdd != null && odd.compareTo(maxOdd) > 0) {
            odd = maxOdd;
        }
        return odd;
    }
}
