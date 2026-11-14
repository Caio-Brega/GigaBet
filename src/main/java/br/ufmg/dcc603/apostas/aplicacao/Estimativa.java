package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Estimativa(int ocorrencias, int historico, int espaco) {
    public BigDecimal probabilidade() {
        return BigDecimal.valueOf(ocorrencias + 1L)
                .divide(BigDecimal.valueOf(historico + (long) espaco), 12, RoundingMode.HALF_UP);
    }
}
