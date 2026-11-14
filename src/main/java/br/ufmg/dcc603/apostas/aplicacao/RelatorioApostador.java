package br.ufmg.dcc603.apostas.aplicacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RelatorioApostador(
        long apostadorId,
        String nome,
        String cpf,
        int quantidade,
        BigDecimal valorTotal,
        int abertas,
        int vencedoras,
        int perdedoras,
        BigDecimal totalPremiosRecebidos,
        Map<Long, List<LinhaRelatorio>> apostasPorPartida) {
}
