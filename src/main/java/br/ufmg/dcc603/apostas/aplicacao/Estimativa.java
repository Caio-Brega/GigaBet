package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Estimativa(
        BigDecimal probabilidade,
        TipoPrevisao tipo,
        int espaco,
        int ocorrencias,
        int historico) {

    public Estimativa(BigDecimal probabilidade, TipoPrevisao tipo, int espaco) {
        this(probabilidade, tipo, espaco, 0, 0);
    }

    public Estimativa(BigDecimal probabilidade, TipoPrevisao tipo) {
        this(probabilidade, tipo, 0, 0, 0);
    }

    public Estimativa(int ocorrencias, int historico, int espaco) {
        this(BigDecimal.valueOf(ocorrencias + 1L)
                .divide(BigDecimal.valueOf(historico + (long) espaco), 12, RoundingMode.HALF_UP),
                null, espaco, ocorrencias, historico);
    }
}
