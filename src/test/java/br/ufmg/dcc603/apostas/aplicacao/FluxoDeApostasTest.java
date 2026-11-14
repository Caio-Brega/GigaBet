package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.Suporte;
import br.ufmg.dcc603.apostas.Sistema;
import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.memoria.ApostaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.EstadoMemoria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluxoDeApostasTest {
    @Test
    void exemploDoDocumentoDebitaECreditaOPremioIntegral() {
        Suporte suporte = new Suporte();
        int[][] placares = {{2, 1}, {1, 0}, {3, 0}, {2, 0}, {0, 1}, {0, 2}, {1, 1}, {2, 2}};
        for (int i = 0; i < placares.length; i++) {
            Partida antiga = suporte.agendar("Flamengo", "Palmeiras", Duration.ofHours(2));
            suporte.encerrar(antiga, placares[i][0], placares[i][1], 3);
            suporte.relogio.avancar(Duration.ofDays(1));
        }
        Partida atual = suporte.agendar("Flamengo", "Palmeiras", Duration.ofHours(3));
        Sessao casa = suporte.apostador("Casa", "casa@email.com", "52998224725", "50.00");
        Sessao placar = suporte.apostador("Placar", "placar@email.com", "39053344705", "10.00");

        ComprovanteAposta vencedor = suporte.sistema.apostas().confirmar(
                casa, PedidoAposta.vencedor(atual.id(), Desfecho.MANDANTE, new BigDecimal("50.00")));
        ComprovanteAposta exato = suporte.sistema.apostas().confirmar(
                placar, PedidoAposta.placar(atual.id(), 2, 1, new BigDecimal("10.00")));

        assertEquals(new BigDecimal("99.00"), vencedor.premioPotencial());
        assertEquals(new BigDecimal("486.00"), exato.premioPotencial());
        assertEquals(new BigDecimal("0.00"), suporte.sistema.autenticacao().exigirApostador(casa).saldo());

        suporte.encerrar(atual, 2, 1, 4);
        assertEquals(new BigDecimal("99.00"), suporte.sistema.autenticacao().exigirApostador(casa).saldo());
        assertEquals(new BigDecimal("486.00"), suporte.sistema.autenticacao().exigirApostador(placar).saldo());
        assertEquals(StatusAposta.VENCEDORA, suporte.sistema.apostas()
                .consultarHistorico(casa, FiltroHistorico.nenhum()).get(0).status());
    }

    @Test
    void cotacaoNaoGravaEConfirmacaoRevalidaSeAPartidaComecar() {
        Suporte suporte = new Suporte();
        Partida partida = suporte.agendar("Bahia", "Vitoria", Duration.ofMinutes(30));
        Sessao sessao = suporte.apostador("Ana", "ana@email.com", "52998224725", "40.00");
        PedidoAposta pedido = PedidoAposta.cartoes(partida.id(), 10, new BigDecimal("15.00"));

        CotacaoAposta cotacao = suporte.sistema.apostas().cotar(sessao, pedido);
        assertEquals(new BigDecimal("40.00"), cotacao.saldoAtual());
        assertTrue(suporte.sistema.apostas().consultarHistorico(sessao, FiltroHistorico.nenhum()).isEmpty());
        assertTrue(suporte.sistema.auditoria().listar().stream()
                .noneMatch(registro -> registro.tipo() == TipoEventoAuditoria.REGISTRO_APOSTA));

        suporte.relogio.avancar(Duration.ofMinutes(31));
        assertEquals(CodigoRegra.PARTIDA_INDISPONIVEL, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().confirmar(sessao, pedido)).codigo());
        assertEquals(new BigDecimal("40.00"), suporte.sistema.autenticacao().exigirApostador(sessao).saldo());
        assertEquals(StatusPartida.EM_ANDAMENTO, suporte.sistema.partidas().detalhar(sessao, partida.id()).status());
    }

    @Test
    void aplicaLimiteDeDezApostasDistintasSaldoEPrevisao() {
        Suporte suporte = new Suporte();
        Sessao sessao = suporte.apostador("Ana", "ana@email.com", "52998224725", "100.00");
        for (int i = 0; i < 10; i++) {
            Partida partida = suporte.agendar("Time A" + i, "Time B" + i, Duration.ofHours(5));
            suporte.sistema.apostas().confirmar(
                    sessao, PedidoAposta.vencedor(partida.id(), Desfecho.EMPATE, new BigDecimal("1.00")));
        }
        Partida extra = suporte.agendar("Time X", "Time Y", Duration.ofHours(5));
        assertEquals(CodigoRegra.LIMITE_APOSTAS_ABERTAS, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().confirmar(
                        sessao, PedidoAposta.vencedor(extra.id(), Desfecho.MANDANTE, new BigDecimal("1.00")))).codigo());
        assertEquals(new BigDecimal("90.00"), suporte.sistema.autenticacao().exigirApostador(sessao).saldo());

        Sessao bia = suporte.apostador("Bia", "bia@email.com", "39053344705", "5.00");
        Partida primeira = suporte.sistema.partidas().listarDisponiveis(bia).get(0);
        suporte.sistema.apostas().confirmar(
                bia, PedidoAposta.vencedor(primeira.id(), Desfecho.MANDANTE, new BigDecimal("1.00")));
        assertEquals(CodigoRegra.APOSTA_DUPLICADA_NA_PARTIDA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().confirmar(
                        bia, PedidoAposta.cartoes(primeira.id(), 2, new BigDecimal("1.00")))).codigo());
        assertEquals(CodigoRegra.SALDO_INSUFICIENTE, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().cotar(
                        bia, PedidoAposta.vencedor(extra.id(), Desfecho.MANDANTE, new BigDecimal("100.00")))).codigo());
        assertEquals(CodigoRegra.PREVISAO_INVALIDA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().cotar(
                        bia, PedidoAposta.placar(extra.id(), -1, 1, new BigDecimal("1.00")))).codigo());
        assertEquals(CodigoRegra.PREVISAO_INVALIDA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().cotar(
                        bia, PedidoAposta.cartoes(extra.id(), 11, new BigDecimal("1.00")))).codigo());
    }

    @Test
    void historicoMostraSomenteAsApostasDoProprioApostador() {
        Suporte suporte = new Suporte();
        Partida partida = suporte.agendar("Cruzeiro", "Atletico", Duration.ofHours(4));
        Sessao ana = suporte.apostador("Ana", "ana@email.com", "52998224725", "30.00");
        Sessao bia = suporte.apostador("Bia", "bia@email.com", "39053344705", "30.00");
        suporte.sistema.apostas().confirmar(
                ana, PedidoAposta.vencedor(partida.id(), Desfecho.MANDANTE, new BigDecimal("5.00")));
        suporte.sistema.apostas().confirmar(
                bia, PedidoAposta.vencedor(partida.id(), Desfecho.VISITANTE, new BigDecimal("8.00")));

        List<ItemHistorico> historico = suporte.sistema.apostas().consultarHistorico(ana, FiltroHistorico.nenhum());
        assertEquals(1, historico.size());
        assertEquals("vitoria do mandante", historico.get(0).previsao());
        List<ItemHistorico> abertas = suporte.sistema.apostas().consultarHistorico(
                ana, new FiltroHistorico(StatusAposta.ABERTA, null, null));
        assertEquals(1, abertas.size());
        assertEquals(CodigoRegra.ACESSO_NEGADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.apostas().consultarHistorico(suporte.admin, FiltroHistorico.nenhum())).codigo());
    }

    @Test
    void resultadoSoEntraEmAndamentoENaoPodeSerAlterado() {
        Suporte suporte = new Suporte();
        Partida partida = suporte.agendar("Gremio", "Inter", Duration.ofHours(2));
        Sessao ana = suporte.apostador("Ana", "ana@email.com", "52998224725", "20.00");
        suporte.sistema.apostas().confirmar(
                ana, PedidoAposta.cartoes(partida.id(), 2, new BigDecimal("10.00")));

        assertEquals(CodigoRegra.PARTIDA_AINDA_NAO_INICIADA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().registrarResultado(suporte.admin, partida.id(), 1, 0, 2)).codigo());
        assertEquals(CodigoRegra.ACESSO_NEGADO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().registrarResultado(ana, partida.id(), 1, 0, 2)).codigo());

        suporte.encerrar(partida, 1, 0, 2);
        assertEquals(new BigDecimal("109.00"), suporte.sistema.autenticacao().exigirApostador(ana).saldo());
        assertEquals(CodigoRegra.PARTIDA_JA_ENCERRADA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().registrarResultado(suporte.admin, partida.id(), 0, 0, 0)).codigo());
        assertEquals(CodigoRegra.RESULTADO_INVALIDO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.partidas().registrarResultado(suporte.admin, partida.id(), -1, 0, 0)).codigo());
    }

    @Test
    void apuracaoFalhaERevertePartidaSaldoStatusEAuditoria() {
        EstadoMemoria estado = new EstadoMemoria();
        ApostaRepositoryMemoria real = new ApostaRepositoryMemoria(estado);
        ApostaRepositoryInstavel instavel = new ApostaRepositoryInstavel(real);
        RelogioMutavelFixo relogio = new RelogioMutavelFixo();
        Sistema sistema = Sistema.criar(relogio.relogio, estado, instavel, new SenhaHasherSha256());
        Sessao admin = sistema.autenticacao().autenticar(
                AutenticacaoService.EMAIL_ADMIN, AutenticacaoService.SENHA_ADMIN);
        sistema.autenticacao().cadastrar(new PedidoCadastro("Ana", "52998224725", "ana@email.com", "senha123", true));
        sistema.autenticacao().cadastrar(new PedidoCadastro("Bia", "39053344705", "bia@email.com", "senha123", true));
        Sessao ana = sistema.autenticacao().autenticar("ana@email.com", "senha123");
        Sessao bia = sistema.autenticacao().autenticar("bia@email.com", "senha123");
        sistema.autenticacao().depositar(ana, new BigDecimal("20.00"));
        sistema.autenticacao().depositar(bia, new BigDecimal("20.00"));
        Partida partida = sistema.partidas().cadastrarPartida(
                admin, "Santos", "Corinthians", relogio.relogio.instant().plus(Duration.ofHours(1)));
        sistema.apostas().confirmar(ana, PedidoAposta.vencedor(partida.id(), Desfecho.MANDANTE, new BigDecimal("10.00")));
        sistema.apostas().confirmar(bia, PedidoAposta.vencedor(partida.id(), Desfecho.VISITANTE, new BigDecimal("10.00")));
        int auditoriaAntes = sistema.auditoria().listar().size();
        relogio.relogio.ajustar(partida.inicio().plusSeconds(30));
        instavel.armar();

        assertThrows(IllegalStateException.class,
                () -> sistema.partidas().registrarResultado(admin, partida.id(), 1, 0, 1));

        Partida depois = sistema.partidas().buscar(partida.id());
        assertEquals(StatusPartida.EM_ANDAMENTO, depois.status());
        assertEquals(null, depois.resultado());
        assertEquals(new BigDecimal("10.00"), sistema.autenticacao().exigirApostador(ana).saldo());
        assertEquals(new BigDecimal("10.00"), sistema.autenticacao().exigirApostador(bia).saldo());
        assertTrue(real.listarPorPartida(partida.id()).stream().allMatch(aposta -> aposta.status() == StatusAposta.ABERTA));
        assertEquals(auditoriaAntes, sistema.auditoria().listar().size());
    }

    @Test
    void perdedorNaoRecebePremio() {
        Suporte suporte = new Suporte();
        Partida partida = suporte.agendar("Botafogo", "Fluminense", Duration.ofHours(1));
        Sessao sessao = suporte.apostador("Ana", "ana@email.com", "52998224725", "25.00");
        suporte.sistema.apostas().confirmar(
                sessao, PedidoAposta.vencedor(partida.id(), Desfecho.EMPATE, new BigDecimal("25.00")));
        suporte.encerrar(partida, 2, 0, 1);
        assertEquals(new BigDecimal("0.00"), suporte.sistema.autenticacao().exigirApostador(sessao).saldo());
        assertEquals(StatusAposta.PERDEDORA, suporte.sistema.apostas()
                .consultarHistorico(sessao, FiltroHistorico.nenhum()).get(0).status());
    }

    private static final class RelogioMutavelFixo {
        private final br.ufmg.dcc603.apostas.tempo.RelogioMutavel relogio =
                new br.ufmg.dcc603.apostas.tempo.RelogioMutavel(java.time.Instant.parse("2026-06-01T15:00:00Z"));
    }

    private static final class ApostaRepositoryInstavel implements ApostaRepository {
        private final ApostaRepositoryMemoria destino;
        private boolean armado;
        private int salvamentos;

        private ApostaRepositoryInstavel(ApostaRepositoryMemoria destino) {
            this.destino = destino;
        }

        private void armar() {
            armado = true;
            salvamentos = 0;
        }

        @Override
        public void salvar(Aposta aposta) {
            if (armado && ++salvamentos >= 2) {
                throw new IllegalStateException("falha de gravacao");
            }
            destino.salvar(aposta);
        }

        @Override
        public List<Aposta> listar() {
            return destino.listar();
        }

        @Override
        public List<Aposta> listarPorApostador(long apostadorId) {
            return destino.listarPorApostador(apostadorId);
        }

        @Override
        public List<Aposta> listarPorPartida(long partidaId) {
            return destino.listarPorPartida(partidaId);
        }

        @Override
        public int contarAbertas(long apostadorId) {
            return destino.contarAbertas(apostadorId);
        }

        @Override
        public boolean existeAberta(long apostadorId, long partidaId) {
            return destino.existeAberta(apostadorId, partidaId);
        }
    }
}
