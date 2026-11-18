package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.Suporte;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelatorioServiceTest {
    @Test
    void relatorioVazioEConsolidadoPorPartidaEApostador() {
        Suporte suporte = new Suporte();
        Partida partida = suporte.agendar("Ceara", "Fortaleza", Duration.ofHours(2));
        RelatorioPartida vazio = suporte.sistema.relatorios().porPartida(suporte.admin, partida.id());
        assertEquals(0, vazio.quantidade());
        assertEquals(new BigDecimal("0.00"), vazio.valorTotal());
        assertEquals(new BigDecimal("0.00"), vazio.totalPremiosPagos());

        Sessao ana = suporte.apostador("Ana Lima", "ana@email.com", "52998224725", "40.00");
        suporte.sistema.apostas().confirmar(
                ana, PedidoAposta.vencedor(partida.id(), Desfecho.MANDANTE, new BigDecimal("10.00")));
        suporte.encerrar(partida, 1, 0, 2);

        RelatorioPartida porPartida = suporte.sistema.relatorios().porPartida(suporte.admin, partida.id());
        assertEquals(1, porPartida.quantidade());
        assertEquals(new BigDecimal("10.00"), porPartida.valorTotal());
        assertEquals(1, porPartida.quantidadePorTipo().get(TipoPrevisao.EQUIPE_VENCEDORA));
        assertEquals(1, porPartida.quantidadePorStatus().get(StatusAposta.VENCEDORA));
        assertEquals(new BigDecimal("20.55"), porPartida.totalPremiosPagos());
        assertEquals("Ana Lima", porPartida.apostas().get(0).apostador());

        RelatorioApostador porApostador = suporte.sistema.relatorios().porApostador(suporte.admin, "529.982.247-25");
        assertEquals(1, porApostador.vencedoras());
        assertEquals(0, porApostador.abertas());
        assertEquals(new BigDecimal("20.55"), porApostador.totalPremiosRecebidos());
        assertEquals(1, porApostador.apostasPorPartida().get(partida.id()).size());

        assertEquals(CodigoRegra.APOSTADOR_NAO_ENCONTRADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.relatorios().porApostador(suporte.admin, "Ninguem")).codigo());
        assertEquals(CodigoRegra.ACESSO_NEGADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.relatorios().porPartida(ana, partida.id())).codigo());
    }

    @Test
    void partidaNoPassadoEquipesIguaisEEquipeDuplicadaSaoRecusadas() {
        Suporte suporte = new Suporte();
        assertEquals(CodigoRegra.DATA_NO_PASSADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().cadastrarPartida(
                        suporte.admin, "A", "B", suporte.relogio.instant().minusSeconds(1))).codigo());
        assertEquals(CodigoRegra.EQUIPES_IGUAIS, assertThrows(RegraNegocioException.class,
                () -> suporte.agendar("Sport", "sport", Duration.ofHours(1))).codigo());
        suporte.sistema.partidas().cadastrarEquipe(suporte.admin, "Sport");
        assertEquals(CodigoRegra.DADOS_INVALIDOS, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().cadastrarEquipe(suporte.admin, "sport")).codigo());
        assertEquals(CodigoRegra.ACESSO_NEGADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().cadastrarPartida(
                        suporte.apostador("Ana", "ana@email.com", "52998224725", null),
                        "A", "B", suporte.relogio.instant().plus(Duration.ofHours(1)))).codigo());
    }
}
