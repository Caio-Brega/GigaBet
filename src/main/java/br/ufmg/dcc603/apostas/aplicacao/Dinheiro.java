package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;

public final class Dinheiro {
    private Dinheiro() {
    }

    public static BigDecimal positivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2) {
            throw new RegraNegocioException(CodigoRegra.VALOR_INVALIDO);
        }
        return valor.setScale(2);
    }

    public static BigDecimal zero() {
        return new BigDecimal("0.00");
    }
}
