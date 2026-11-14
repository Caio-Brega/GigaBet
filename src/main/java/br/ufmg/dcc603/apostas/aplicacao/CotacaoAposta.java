package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;

public record CotacaoAposta(BigDecimal probabilidade, BigDecimal premioPotencial, BigDecimal saldoAtual) {
}
