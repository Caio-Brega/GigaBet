package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;

public record ParametrosCalculo(BigDecimal fatorMargem, BigDecimal oddsMinima) {
    public static final ParametrosCalculo PADRAO =
            new ParametrosCalculo(new BigDecimal("0.90"), new BigDecimal("1.01"));

    public ParametrosCalculo {
        if (fatorMargem == null || oddsMinima == null) {
            throw new IllegalArgumentException("Parametros de calculo obrigatorios");
        }
    }
}
