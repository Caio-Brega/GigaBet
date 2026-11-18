package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;

public record ParametrosCalculo(
        BigDecimal fatorMargem,
        BigDecimal oddsMinima,
        BigDecimal maxOddPlacarExato,
        BigDecimal maxOddCartoes,
        BigDecimal maxOddEquipeVencedora,
        BigDecimal maxPayout) {

    public static final BigDecimal PADRAO_FATOR_MARGEM = new BigDecimal("0.90");
    public static final BigDecimal PADRAO_ODDS_MINIMA = new BigDecimal("1.01");
    public static final BigDecimal PADRAO_MAX_ODD_PLACAR = new BigDecimal("25.00");
    public static final BigDecimal PADRAO_MAX_ODD_CARTOES = new BigDecimal("15.00");
    public static final BigDecimal PADRAO_MAX_ODD_VENCEDOR = new BigDecimal("10.00");
    public static final BigDecimal PADRAO_MAX_PAYOUT = new BigDecimal("100000.00");

    public static final ParametrosCalculo PADRAO = new ParametrosCalculo(
            PADRAO_FATOR_MARGEM,
            PADRAO_ODDS_MINIMA,
            PADRAO_MAX_ODD_PLACAR,
            PADRAO_MAX_ODD_CARTOES,
            PADRAO_MAX_ODD_VENCEDOR,
            PADRAO_MAX_PAYOUT
    );

    public ParametrosCalculo(BigDecimal fatorMargem, BigDecimal oddsMinima) {
        this(fatorMargem, oddsMinima,
                PADRAO_MAX_ODD_PLACAR, PADRAO_MAX_ODD_CARTOES, PADRAO_MAX_ODD_VENCEDOR, PADRAO_MAX_PAYOUT);
    }

    public ParametrosCalculo {
        if (fatorMargem == null || oddsMinima == null) {
            throw new IllegalArgumentException("Parametros de calculo obrigatorios");
        }
    }

    public BigDecimal maxOdd(TipoPrevisao tipo) {
        if (tipo == null) {
            return PADRAO_MAX_ODD_PLACAR;
        }
        return switch (tipo) {
            case PLACAR_EXATO -> maxOddPlacarExato != null ? maxOddPlacarExato : PADRAO_MAX_ODD_PLACAR;
            case CARTOES_AMARELOS -> maxOddCartoes != null ? maxOddCartoes : PADRAO_MAX_ODD_CARTOES;
            case EQUIPE_VENCEDORA -> maxOddEquipeVencedora != null ? maxOddEquipeVencedora : PADRAO_MAX_ODD_VENCEDOR;
        };
    }
}
