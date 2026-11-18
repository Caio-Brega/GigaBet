package br.ufmg.dcc603.apostas.aplicacao;

public record ResultadoSincronizacaoCompleta(
        ResultadoSincronizacaoAgendadas agendadas,
        ResultadoSincronizacaoHistorico historico) {
}
