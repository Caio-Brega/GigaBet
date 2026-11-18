package br.ufmg.dcc603.apostas.aplicacao;

public record ResultadoSincronizacaoHistorico(
        boolean sucesso,
        String mensagem,
        int totalEventos,
        int partidasApuradas,
        int apostasApuradas,
        int partidasHistoricoCriadas,
        int partidasJaEncerradas,
        int partidasEmAndamento) {
}
