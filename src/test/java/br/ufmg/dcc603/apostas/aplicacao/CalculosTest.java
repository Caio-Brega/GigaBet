package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.ApostaCartoesAmarelos;
import br.ufmg.dcc603.apostas.dominio.ApostaEquipeVencedora;
import br.ufmg.dcc603.apostas.dominio.ApostaPlacarExato;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculosTest {
    private final CalculadoraProbabilidade probabilidade = CalculadoraProbabilidade.padrao();
    private final CalculadoraPremio premio = new CalculadoraPremio(ParametrosCalculo.PADRAO);
    private final Equipe casa = new Equipe(1, "Flamengo");
    private final Equipe fora = new Equipe(2, "Palmeiras");

    @Test
    void exemplosDaSprintDois() {
        List<Partida> historico = historicoDocumentado();
        Partida atual = new Partida(99, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));

        Estimativa vencedor = probabilidade.estimar(
                atual, PedidoAposta.vencedor(atual.id(), Desfecho.MANDANTE, new BigDecimal("50.00")), historico);
        assertEquals(4, vencedor.ocorrencias());
        assertEquals(8, vencedor.historico());
        assertEquals(3, vencedor.espaco());
        assertEquals(new BigDecimal("99.00"), premio.calcular(new BigDecimal("50.00"), vencedor));

        Estimativa placar = probabilidade.estimar(
                atual, PedidoAposta.placar(atual.id(), 2, 1, new BigDecimal("10.00")), historico);
        assertEquals(1, placar.ocorrencias());
        assertEquals(8, placar.historico());
        assertEquals(100, placar.espaco());
        assertEquals(new BigDecimal("486.00"), premio.calcular(new BigDecimal("10.00"), placar));
    }

    @Test
    void semHistoricoUsaUmSobreK() {
        Partida atual = new Partida(1, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa vencedor = probabilidade.estimar(
                atual, PedidoAposta.vencedor(1, Desfecho.EMPATE, new BigDecimal("10.00")), List.of());
        assertEquals(0, vencedor.historico());
        assertEquals(0, vencedor.probabilidade().compareTo(new BigDecimal("0.333333333333")));
        assertEquals(new BigDecimal("27.00"), premio.calcular(new BigDecimal("10.00"), vencedor));

        Estimativa cartoes = probabilidade.estimar(
                atual, PedidoAposta.cartoes(1, 4, new BigDecimal("11.00")), List.of());
        assertEquals(11, cartoes.espaco());
        assertEquals(new BigDecimal("108.90"), premio.calcular(new BigDecimal("11.00"), cartoes));
    }

    @Test
    void oddsMinimaQuandoAProbabilidadeEAlta() {
        List<Partida> historico = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            historico.add(encerrada(i + 1, 1, 0, 1));
        }
        Partida atual = new Partida(100, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa estimativa = probabilidade.estimar(
                atual, PedidoAposta.vencedor(100, Desfecho.MANDANTE, new BigDecimal("100.00")), historico);
        assertEquals(new BigDecimal("101.00"), premio.calcular(new BigDecimal("100.00"), estimativa));
    }

    @Test
    void premioArredondaCentavos() {
        List<Partida> historico = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            historico.add(encerrada(i + 1, 1, 0, 0));
        }
        for (int i = 0; i < 2; i++) {
            historico.add(encerrada(i + 7, 0, 1, 0));
        }
        Partida atual = new Partida(20, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa estimativa = probabilidade.estimar(
                atual, PedidoAposta.vencedor(20, Desfecho.MANDANTE, new BigDecimal("10.00")), historico);
        assertEquals(6, estimativa.ocorrencias());
        assertEquals(8, estimativa.historico());
        assertEquals(new BigDecimal("14.14"), premio.calcular(new BigDecimal("10.00"), estimativa));
    }

    @Test
    void confrontoInvertidoEntraNoHistorico() {
        Partida invertida = new Partida(3, fora, casa, Instant.parse("2026-01-02T19:00:00Z"));
        invertida.encerrar(new ResultadoPartida(0, 2, 1));
        Partida atual = new Partida(4, casa, fora, Instant.parse("2026-02-01T19:00:00Z"));
        Estimativa estimativa = probabilidade.estimar(
                atual, PedidoAposta.vencedor(4, Desfecho.VISITANTE, new BigDecimal("10")), List.of(invertida));
        assertEquals(1, estimativa.historico());
        assertEquals(1, estimativa.ocorrencias());
    }

    @Test
    void avaliacaoDosTresTiposEAgrupamentoDeCartoes() {
        ResultadoPartida resultado = new ResultadoPartida(2, 1, 12);
        Instant agora = Instant.parse("2026-01-01T00:00:00Z");
        assertTrue(new ApostaEquipeVencedora(1, 1, 1, decimal("10"), decimal("0.3"), decimal("30"), agora, Desfecho.MANDANTE)
                .avaliar(resultado));
        assertFalse(new ApostaEquipeVencedora(2, 1, 1, decimal("10"), decimal("0.3"), decimal("30"), agora, Desfecho.EMPATE)
                .avaliar(resultado));
        assertTrue(new ApostaPlacarExato(3, 1, 1, decimal("10"), decimal("0.01"), decimal("100"), agora, 2, 1)
                .avaliar(resultado));
        assertFalse(new ApostaPlacarExato(4, 1, 1, decimal("10"), decimal("0.01"), decimal("100"), agora, 1, 2)
                .avaliar(resultado));
        assertTrue(new ApostaCartoesAmarelos(5, 1, 1, decimal("10"), decimal("0.1"), decimal("20"), agora, 10)
                .avaliar(resultado));
        assertFalse(new ApostaCartoesAmarelos(6, 1, 1, decimal("10"), decimal("0.1"), decimal("20"), agora, 9)
                .avaliar(resultado));
    }

    private List<Partida> historicoDocumentado() {
        int[][] placares = {{2, 1}, {1, 0}, {3, 0}, {2, 0}, {0, 1}, {0, 2}, {1, 1}, {2, 2}};
        List<Partida> partidas = new ArrayList<>();
        for (int i = 0; i < placares.length; i++) {
            partidas.add(encerrada(i + 1, placares[i][0], placares[i][1], 2));
        }
        return partidas;
    }

    private Partida encerrada(long id, int golsMandante, int golsVisitante, int cartoes) {
        Partida partida = new Partida(id, casa, fora, Instant.parse("2026-01-01T19:00:00Z").plusSeconds(id));
        partida.encerrar(new ResultadoPartida(golsMandante, golsVisitante, cartoes));
        return partida;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
