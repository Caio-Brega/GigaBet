package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RelatorioPartida(
        long partidaId,
        String partida,
        int quantidade,
        BigDecimal valorTotal,
        Map<TipoPrevisao, Integer> quantidadePorTipo,
        Map<StatusAposta, Integer> quantidadePorStatus,
        BigDecimal totalPremiosPagos,
        List<LinhaRelatorio> apostas) {
}
