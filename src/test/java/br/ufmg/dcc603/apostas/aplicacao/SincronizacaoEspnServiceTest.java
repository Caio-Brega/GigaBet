package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.Sistema;
import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.integracao.espn.EspnBrasileiraoClient;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnCompetitorDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnDetailDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnEventDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnScoreboardDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnSummaryDto;
import br.ufmg.dcc603.apostas.tempo.RelogioMutavel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizacaoEspnServiceTest {

    private RelogioMutavel relogio;
    private Sistema sistema;
    private FakeEspnClient fakeEspn;
    private Sessao admin;

    @BeforeEach
    void setUp() {
        relogio = new RelogioMutavel(Instant.parse("2026-11-20T12:00:00Z"));
        fakeEspn = new FakeEspnClient();
        sistema = Sistema.criar(relogio, fakeEspn);
        admin = sistema.autenticacao().autenticar(AutenticacaoService.EMAIL_ADMIN, AutenticacaoService.SENHA_ADMIN);
    }

    @Test
    void sincronizaProximosJogosCadastraEquipesEPartidasEvitandoDuplicatas() {
        // Simula evento agendado da ESPN daqui a 2 dias
        Instant dataJogo = relogio.instant().plusSeconds(86400 * 2);
        EspnEventDto evento1 = new EspnEventDto(
                "1001",
                dataJogo,
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, null),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, null),
                List.of(),
                0
        );

        // Simula evento com nome que precisa de normalização ("Atlético Mineiro" -> "Atlético-MG")
        EspnEventDto evento2 = new EspnEventDto(
                "1002",
                dataJogo.plusSeconds(3600),
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("3", "Atlético Mineiro", "Atlético Mineiro", "CAM", true, null),
                new EspnCompetitorDto("4", "Cruzeiro", "Cruzeiro EC", "CRU", false, null),
                List.of(),
                0
        );

        fakeEspn.adicionarEvento(evento1);
        fakeEspn.adicionarEvento(evento2);

        // 1ª sincronização
        ResultadoSincronizacaoAgendadas res = sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        assertTrue(res.sucesso());
        assertEquals(2, res.partidasCriadas());
        assertEquals(0, res.partidasJaExistentes());
        assertEquals(4, res.equipesCriadas());
        assertEquals(List.of("20261120-20261204"), fakeEspn.datasConsultadas());

        // Verifica equipes cadastradas
        Optional<Equipe> atletico = sistema.partidas().listarEquipes().stream()
                .filter(e -> e.nome().equals("Atlético-MG"))
                .findFirst();
        assertTrue(atletico.isPresent());

        // Verifica partidas no catálogo
        List<Partida> partidas = sistema.partidas().listarCatalogo();
        assertEquals(2, partidas.size());
        Partida p1 = partidas.stream().filter(p -> "1001".equals(p.externalId())).findFirst().orElseThrow();
        assertEquals("Flamengo x Palmeiras", p1.descricao());
        assertEquals(StatusPartida.AGENDADA, p1.status());
        assertTrue(p1.aceitaApostas(relogio.instant()));

        // 2ª sincronização (não deve duplicar partidas)
        ResultadoSincronizacaoAgendadas res2 = sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        assertTrue(res2.sucesso());
        assertEquals(0, res2.partidasCriadas());
        assertEquals(2, res2.partidasJaExistentes());
        assertEquals(0, res2.equipesCriadas());
        assertEquals(2, sistema.partidas().listarCatalogo().size());
    }

    @Test
    void ignoraPartidasAgendadasNoPassadoParaAberturaDeApostas() {
        // Evento agendado mas com data no passado (não deve abrir para apostas)
        Instant passado = relogio.instant().minusSeconds(3600);
        EspnEventDto eventoPassado = new EspnEventDto(
                "1003",
                passado,
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, null),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, null),
                List.of(),
                0
        );
        fakeEspn.adicionarEvento(eventoPassado);

        ResultadoSincronizacaoAgendadas res = sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        assertTrue(res.sucesso());
        assertEquals(0, res.partidasCriadas());
        assertTrue(sistema.partidas().listarCatalogo().isEmpty());
    }

    @Test
    void atualizaPartidaEmAndamentoEFechaJanelaDeApostas() {
        Instant dataJogo = relogio.instant().plusSeconds(1800);
        EspnEventDto agendado = new EspnEventDto(
                "2001",
                dataJogo,
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("1", "Vasco da Gama", "Vasco da Gama", "VAS", true, null),
                new EspnCompetitorDto("2", "Botafogo", "Botafogo FR", "BOT", false, null),
                List.of(),
                0
        );
        fakeEspn.adicionarEvento(agendado);
        sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);

        Partida partida = sistema.partidas().listarCatalogo().get(0);
        assertEquals(StatusPartida.AGENDADA, partida.status());
        assertTrue(partida.aceitaApostas(relogio.instant()));

        // A partida agora entrou em andamento na ESPN
        fakeEspn.limpar();
        EspnEventDto emAndamento = new EspnEventDto(
                "2001",
                dataJogo,
                "STATUS_IN_PROGRESS",
                "1st Half",
                new EspnCompetitorDto("1", "Vasco", "Vasco", "VAS", true, 0),
                new EspnCompetitorDto("2", "Botafogo", "Botafogo", "BOT", false, 0),
                List.of(),
                0
        );
        fakeEspn.adicionarEvento(emAndamento);

        sistema.sincronizacaoEspn().sincronizarHistoricoEApuracao(admin, null);

        Partida atualizada = sistema.partidas().buscar(partida.id());
        assertEquals(StatusPartida.EM_ANDAMENTO, atualizada.status());
        assertFalse(atualizada.aceitaApostas(relogio.instant()));
    }

    @Test
    void apuraApostasAutomaticamenteAoEncerrarPartida() {
        // 1. Cadastra partida agendada via sincronização
        Instant dataJogo = relogio.instant().plusSeconds(3600);
        EspnEventDto agendado = new EspnEventDto(
                "3001",
                dataJogo,
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, null),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, null),
                List.of(),
                0
        );
        fakeEspn.adicionarEvento(agendado);
        sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        Partida partida = sistema.partidas().listarCatalogo().get(0);

        // 2. Cria apostador e realiza aposta no MANDANTE (Flamengo)
        sistema.autenticacao().cadastrar(new PedidoCadastro("Lucas Silva", "52998224725", "lucas@email.com", "senha123", true));
        Sessao lucas = sistema.autenticacao().autenticar("lucas@email.com", "senha123");
        sistema.autenticacao().depositar(lucas, new BigDecimal("100.00"));
        sistema.apostas().confirmar(lucas, PedidoAposta.vencedor(partida.id(), Desfecho.MANDANTE, new BigDecimal("40.00")));

        Apostador apostadorAposAposta = sistema.autenticacao().exigirApostador(lucas);
        assertEquals(0, apostadorAposAposta.saldo().compareTo(new BigDecimal("60.00")));

        // 3. Simula fim de jogo na ESPN: Flamengo 2 x 1 Palmeiras, com 5 cartões amarelos
        fakeEspn.limpar();
        List<EspnDetailDto> detalhes = List.of(
                new EspnDetailDto(true, "Yellow Card", "", "", "1", "Gerson", 20.0),
                new EspnDetailDto(true, "Yellow Card", "", "", "1", "Pulgar", 44.0),
                new EspnDetailDto(true, "Cartão Amarelo", "", "", "2", "Gomez", 60.0),
                new EspnDetailDto(true, "Yellow Card", "", "", "2", "Veiga", 75.0),
                new EspnDetailDto(true, "Yellow Card", "", "", "2", "Rony", 88.0)
        );
        EspnEventDto finalizado = new EspnEventDto(
                "3001",
                dataJogo,
                "STATUS_FULL_TIME",
                "Full Time",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, 2),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, 1),
                detalhes,
                5
        );
        fakeEspn.adicionarEvento(finalizado);

        // Executa sincronização de histórico e apuração
        ResultadoSincronizacaoHistorico res = sistema.sincronizacaoEspn().sincronizarHistoricoEApuracao(admin, null);
        assertTrue(res.sucesso());
        assertEquals(1, res.partidasApuradas());
        assertEquals(1, res.apostasApuradas());

        // Verifica que a partida foi encerrada com o resultado exato
        Partida encerrada = sistema.partidas().buscar(partida.id());
        assertEquals(StatusPartida.ENCERRADA, encerrada.status());
        assertNotNull(encerrada.resultado());
        assertEquals(2, encerrada.resultado().golsMandante());
        assertEquals(1, encerrada.resultado().golsVisitante());
        assertEquals(5, encerrada.resultado().cartoesAmarelos());

        // Verifica que o apostador recebeu o prêmio creditado em seu saldo
        Apostador apostadorPremiado = sistema.autenticacao().exigirApostador(lucas);
        assertTrue(apostadorPremiado.saldo().compareTo(new BigDecimal("60.00")) > 0);

        List<ItemHistorico> historico = sistema.apostas().consultarHistorico(lucas, FiltroHistorico.nenhum());
        assertEquals(1, historico.size());
        assertEquals(StatusAposta.VENCEDORA, historico.get(0).status());
    }

    @Test
    void apuraApostaDeCartoesAmarelosAgrupadosAcimaDe10() {
        // Cadastra partida
        Instant dataJogo = relogio.instant().plusSeconds(3600);
        fakeEspn.adicionarEvento(new EspnEventDto(
                "4001",
                dataJogo,
                "STATUS_SCHEDULED",
                "Scheduled",
                new EspnCompetitorDto("1", "Santos", "Santos", "SAN", true, null),
                new EspnCompetitorDto("2", "São Paulo", "São Paulo", "SAO", false, null),
                List.of(),
                0
        ));
        sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        Partida partida = sistema.partidas().listarCatalogo().get(0);

        // Apostador aposta em "10 ou mais cartões amarelos" (total = 10)
        sistema.autenticacao().cadastrar(new PedidoCadastro("Julia Costa", "39053344705", "julia@email.com", "senha123", true));
        Sessao julia = sistema.autenticacao().autenticar("julia@email.com", "senha123");
        sistema.autenticacao().depositar(julia, new BigDecimal("50.00"));
        sistema.apostas().confirmar(julia, PedidoAposta.cartoes(partida.id(), 10, new BigDecimal("20.00")));

        // Jogo termina com 12 cartões amarelos (> 10)
        fakeEspn.limpar();
        List<EspnDetailDto> dozeCartoes = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            dozeCartoes.add(new EspnDetailDto(true, "Yellow Card", "", "", "1", "Jogador " + i, i * 5));
        }
        fakeEspn.adicionarEvento(new EspnEventDto(
                "4001",
                dataJogo,
                "STATUS_FULL_TIME",
                "Full Time",
                new EspnCompetitorDto("1", "Santos", "Santos", "SAN", true, 0),
                new EspnCompetitorDto("2", "São Paulo", "São Paulo", "SAO", false, 0),
                dozeCartoes,
                12
        ));

        sistema.sincronizacaoEspn().sincronizarHistoricoEApuracao(admin, null);

        // Regra RN: valores acima de 10 contam como 10 no agrupamento, logo a aposta no 10 vence!
        Partida p = sistema.partidas().buscar(partida.id());
        assertEquals(10, p.resultado().cartoesAmarelos());
        assertEquals(10, p.resultado().cartoesAgrupados());

        Apostador juliaPremiada = sistema.autenticacao().exigirApostador(julia);
        assertTrue(juliaPremiada.saldo().compareTo(new BigDecimal("30.00")) > 0);
    }

    @Test
    void importaPartidasHistoricasQueAlimentamMotorDePoisson() {
        // Nenhuma partida anterior existe.
        assertTrue(sistema.partidas().listarCatalogo().isEmpty());

        // Importa 3 partidas finalizadas do Brasileirão pela ESPN
        Instant data1 = relogio.instant().minusSeconds(86400 * 5);
        Instant data2 = relogio.instant().minusSeconds(86400 * 3);
        Instant data3 = relogio.instant().minusSeconds(86400 * 1);

        fakeEspn.adicionarEvento(new EspnEventDto(
                "5001", data1, "STATUS_FULL_TIME", "FT",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, 3),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, 1),
                List.of(), 4
        ));
        fakeEspn.adicionarEvento(new EspnEventDto(
                "5002", data2, "STATUS_FULL_TIME", "FT",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, 2),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, 0),
                List.of(), 3
        ));
        fakeEspn.adicionarEvento(new EspnEventDto(
                "5003", data3, "STATUS_FULL_TIME", "FT",
                new EspnCompetitorDto("1", "Flamengo", "Flamengo", "FLA", true, 1),
                new EspnCompetitorDto("2", "Palmeiras", "Palmeiras", "PAL", false, 2),
                List.of(), 5
        ));

        ResultadoSincronizacaoHistorico res = sistema.sincronizacaoEspn().sincronizarHistoricoEApuracao(admin, null);
        assertTrue(res.sucesso());
        assertEquals(3, res.partidasHistoricoCriadas());

        // Cria uma próxima partida entre Flamengo e Palmeiras
        Partida proxima = sistema.partidas().cadastrarPartida(
                admin, "Flamengo", "Palmeiras", relogio.instant().plusSeconds(86400));

        // Calcula estatísticas para Poisson com base em todas as partidas do repositório
        List<Partida> todas = sistema.partidas().listarCatalogo();
        EstatisticasPartida stats = EstatisticasPartida.calcular(proxima, todas);

        assertFalse(stats.isColdStartMandante());
        assertFalse(stats.isColdStartVisitante());
        // Média de gols pró do Flamengo como mandante: (3 + 2 + 1) / 3 = 2.0
        assertEquals(2.0, stats.mediaGolsProMandante(), 0.001);
        assertTrue(stats.lambdaMandante() > 0.1);
        assertTrue(stats.lambdaCartoes() > 0.1);
    }

    @Test
    void fallbackGraciosoRegistraErroSemDerrubarSistema() {
        fakeEspn.simularFalha("API da ESPN fora do ar (503 Service Unavailable)");

        ResultadoSincronizacaoAgendadas resAgendadas = sistema.sincronizacaoEspn().sincronizarProximosJogos(admin);
        assertFalse(resAgendadas.sucesso());
        assertTrue(resAgendadas.mensagem().contains("503"));

        ResultadoSincronizacaoHistorico resHistorico = sistema.sincronizacaoEspn().sincronizarHistoricoEApuracao(admin, null);
        assertFalse(resHistorico.sucesso());
        assertTrue(resHistorico.mensagem().contains("503"));

        // O sistema continua operacional
        assertTrue(sistema.partidas().listarCatalogo().isEmpty());
    }

    private static class FakeEspnClient extends EspnBrasileiraoClient {
        private final List<EspnEventDto> eventos = new ArrayList<>();
        private final List<String> datas = new ArrayList<>();
        private String erroSimulado = null;

        public void adicionarEvento(EspnEventDto evento) {
            this.eventos.add(evento);
        }

        public void limpar() {
            this.eventos.clear();
            this.datas.clear();
            this.erroSimulado = null;
        }

        public List<String> datasConsultadas() {
            return List.copyOf(datas);
        }

        public void simularFalha(String mensagem) {
            this.erroSimulado = mensagem;
        }

        @Override
        public EspnScoreboardDto buscarScoreboard(String dates) {
            datas.add(dates);
            if (erroSimulado != null) {
                throw new br.ufmg.dcc603.apostas.integracao.espn.EspnApiException(erroSimulado);
            }
            return new EspnScoreboardDto(eventos);
        }

        @Override
        public Optional<EspnSummaryDto> buscarResumoSeguro(String eventId) {
            if (erroSimulado != null) {
                return Optional.empty();
            }
            for (EspnEventDto e : eventos) {
                if (e.id().equals(eventId)) {
                    return Optional.of(new EspnSummaryDto(eventId, e.details(), e.yellowCards()));
                }
            }
            return Optional.empty();
        }
    }
}
