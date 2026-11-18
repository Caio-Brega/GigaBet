package br.ufmg.dcc603.apostas.aplicacao;

public record ResultadoSincronizacaoAgendadas(
        boolean sucesso,
        String mensagem,
        int totalEventos,
        int partidasCriadas,
        int partidasJaExistentes,
        int equipesCriadas) {
}
