package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Administrador;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.integracao.espn.EspnApiException;
import br.ufmg.dcc603.apostas.integracao.espn.EspnBrasileiraoClient;
import br.ufmg.dcc603.apostas.integracao.espn.NormalizadorEquipes;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnEventDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnScoreboardDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnSummaryDto;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public final class SincronizacaoEspnService {
    private final EspnBrasileiraoClient espnClient;
    private final EquipeRepository equipes;
    private final PartidaRepository partidas;
    private final UnidadeDeTrabalho unidade;
    private final AutenticacaoService autenticacao;
    private final ApuracaoService apuracao;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public SincronizacaoEspnService(
            EspnBrasileiraoClient espnClient,
            EquipeRepository equipes,
            PartidaRepository partidas,
            UnidadeDeTrabalho unidade,
            AutenticacaoService autenticacao,
            ApuracaoService apuracao,
            AuditoriaService auditoria,
            Clock relogio) {
        this.espnClient = Objects.requireNonNull(espnClient);
        this.equipes = Objects.requireNonNull(equipes);
        this.partidas = Objects.requireNonNull(partidas);
        this.unidade = Objects.requireNonNull(unidade);
        this.autenticacao = Objects.requireNonNull(autenticacao);
        this.apuracao = Objects.requireNonNull(apuracao);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public EspnBrasileiraoClient espnClient() {
        return espnClient;
    }

    /**
     * Sincroniza partidas agendadas (STATUS_SCHEDULED) para o intervalo [HOJE, HOJE + 14 DIAS].
     * Garante que apenas partidas com STATUS_SCHEDULED e data futura fiquem abertas para apostas.
     * Evita duplicatas atraves do external_id (event.id da ESPN).
     */
    public ResultadoSincronizacaoAgendadas sincronizarProximosJogos(Sessao sessao) {
        Administrador admin = sessao != null ? autenticacao.exigirAdministrador(sessao) : null;
        Long adminId = admin != null ? admin.id() : null;

        LocalDate hoje = LocalDate.ofInstant(relogio.instant(), ZoneOffset.UTC);
        LocalDate daqui14 = hoje.plusDays(14);
        DateTimeFormatter fmt = DateTimeFormatter.BASIC_ISO_DATE;
        String dates = hoje.format(fmt) + "-" + daqui14.format(fmt);

        EspnScoreboardDto scoreboard;
        try {
            scoreboard = espnClient.buscarScoreboard(dates);
        } catch (EspnApiException e) {
            String msg = "Falha ao buscar partidas agendadas na ESPN: " + e.getMessage();
            auditoria.registrar(TipoEventoAuditoria.SINCRONIZACAO_ESPN, adminId, msg);
            return new ResultadoSincronizacaoAgendadas(false, msg, 0, 0, 0, 0);
        }

        int total = scoreboard.events().size();
        int criadas = 0;
        int existentes = 0;
        int equipesNovas = 0;

        Instant agora = relogio.instant();

        for (EspnEventDto event : scoreboard.events()) {
            if (!event.isScheduled() || event.date() == null || !event.date().isAfter(agora)) {
                continue;
            }

            if (event.id() == null || event.id().isBlank()
                    || event.homeCompetitor() == null || event.awayCompetitor() == null) {
                continue;
            }

            String nomeMandante = NormalizadorEquipes.normalizar(event.homeCompetitor().displayName());
            String nomeVisitante = NormalizadorEquipes.normalizar(event.awayCompetitor().displayName());
            if (nomeMandante.isBlank() || nomeVisitante.isBlank()
                    || nomeMandante.equalsIgnoreCase(nomeVisitante)) {
                continue;
            }

            String eventId = event.id();
            Optional<Partida> partidaOpt = partidas.buscarPorExternalId(eventId);
            if (partidaOpt.isPresent()) {
                existentes++;
                continue;
            }

            try {
                int novasEquipes = unidade.executar(() -> {
                    int novas = 0;
                    if (equipes.buscarPorNome(nomeMandante).isEmpty()) novas++;
                    if (equipes.buscarPorNome(nomeVisitante).isEmpty()) novas++;
                    Equipe m = obterOuCriarEquipe(nomeMandante);
                    Equipe v = obterOuCriarEquipe(nomeVisitante);

                    Partida partida = new Partida(unidade.proximoId(), m, v, event.date(), eventId);
                    partidas.salvar(partida);

                    auditoria.registrar(TipoEventoAuditoria.CADASTRO_PARTIDA, adminId,
                            "Partida ESPN agendada: " + partida.descricao() + " (ESPN ID: " + eventId + ")");
                    return novas;
                });
                criadas++;
                equipesNovas += novasEquipes;
            } catch (Exception e) {
                System.err.println("[Sincronizacao ESPN] Erro ao cadastrar partida agendada: " + e.getMessage());
            }
        }

        String msg = "Sincronizacao de proximos jogos concluida. " + criadas
                + " partidas cadastradas, " + existentes + " ja existentes.";
        auditoria.registrar(TipoEventoAuditoria.SINCRONIZACAO_ESPN, adminId, msg);

        return new ResultadoSincronizacaoAgendadas(true, msg, total, criadas, existentes, equipesNovas);
    }

    /**
     * Sincroniza partidas finalizadas (STATUS_FULL_TIME):
     * 1. Alimenta o historico recente de gols e cartoes amarelos para o calculo de Poisson.
     * 2. Dispara a apuracao atomica das apostas abertas vinculadas aquela partida assim que ela encerrar.
     * 3. Atualiza partidas em andamento (fechando a janela de apostas).
     */
    public ResultadoSincronizacaoHistorico sincronizarHistoricoEApuracao(Sessao sessao, String intervaloDatas) {
        Administrador admin = sessao != null ? autenticacao.exigirAdministrador(sessao) : null;
        Long adminId = admin != null ? admin.id() : null;

        String dates = intervaloDatas;
        if (dates == null || dates.isBlank()) {
            LocalDate hoje = LocalDate.ofInstant(relogio.instant(), ZoneOffset.UTC);
            LocalDate passado = hoje.minusDays(30);
            DateTimeFormatter fmt = DateTimeFormatter.BASIC_ISO_DATE;
            dates = passado.format(fmt) + "-" + hoje.format(fmt);
        }

        EspnScoreboardDto scoreboard;
        try {
            scoreboard = espnClient.buscarScoreboard(dates);
        } catch (EspnApiException e) {
            String msg = "Falha ao buscar historico na ESPN: " + e.getMessage();
            auditoria.registrar(TipoEventoAuditoria.SINCRONIZACAO_ESPN, adminId, msg);
            return new ResultadoSincronizacaoHistorico(false, msg, 0, 0, 0, 0, 0, 0);
        }

        int total = scoreboard.events().size();
        int partidasApuradas = 0;
        int apostasApuradas = 0;
        int historicoCriado = 0;
        int jaEncerradas = 0;
        int emAndamento = 0;

        for (EspnEventDto event : scoreboard.events()) {
            if (event.id() == null || event.id().isBlank()
                    || event.homeCompetitor() == null || event.awayCompetitor() == null) {
                continue;
            }

            String eventId = event.id();
            Optional<Partida> partidaOpt = partidas.buscarPorExternalId(eventId);

            if (event.isInProgress()) {
                if (partidaOpt.isPresent()) {
                    Partida p = partidaOpt.get();
                    if (p.status() == StatusPartida.AGENDADA) {
                        p.marcarEmAndamento();
                        partidas.salvar(p);
                        emAndamento++;
                    }
                }
                continue;
            }

            if (event.isFullTime()) {
                Integer gm = event.homeScore();
                Integer gv = event.awayScore();
                if (gm == null || gv == null) {
                    continue;
                }
                int golsMandante = gm;
                int golsVisitante = gv;

                int cartoesEncontrados = event.yellowCards();
                if (cartoesEncontrados == 0) {
                    Optional<EspnSummaryDto> resumo = espnClient.buscarResumoSeguro(eventId);
                    if (resumo.isPresent()) {
                        cartoesEncontrados = resumo.get().yellowCards();
                    }
                }

                final int totalCartoes = Math.min(Math.max(cartoesEncontrados, 0), 10);
                ResultadoPartida resultado = new ResultadoPartida(golsMandante, golsVisitante, totalCartoes);

                if (partidaOpt.isPresent()) {
                    Partida partida = partidaOpt.get();
                    if (partida.status() == StatusPartida.ENCERRADA) {
                        jaEncerradas++;
                        continue;
                    }

                    ResumoApuracao resumoApuracao = unidade.executar(() -> {
                        partida.encerrar(resultado);
                        partidas.salvar(partida);
                        ResumoApuracao res = apuracao.apurar(partida);

                        auditoria.registrar(TipoEventoAuditoria.REGISTRO_RESULTADO, adminId,
                                "Resultado ESPN registrado para " + partida.descricao() + ": "
                                        + golsMandante + " x " + golsVisitante + ", cartoes: " + totalCartoes);
                        auditoria.registrar(TipoEventoAuditoria.APURACAO, adminId,
                                "Apuracao automatica ESPN para " + partida.descricao() + ": "
                                        + res.apostasApuradas() + " apostas, "
                                        + res.vencedoras() + " vencedoras, "
                                        + res.perdedoras() + " perdedoras");
                        return res;
                    });
                    partidasApuradas++;
                    apostasApuradas += resumoApuracao.apostasApuradas();
                } else {
                    boolean importada = unidade.executar(() -> {
                        String nomeM = NormalizadorEquipes.normalizar(event.homeCompetitor().displayName());
                        String nomeV = NormalizadorEquipes.normalizar(event.awayCompetitor().displayName());
                        if (nomeM.isBlank() || nomeV.isBlank() || nomeM.equalsIgnoreCase(nomeV)) {
                            return false;
                        }
                        Equipe m = obterOuCriarEquipe(nomeM);
                        Equipe v = obterOuCriarEquipe(nomeV);

                        Instant dataPartida = event.date() != null ? event.date() : relogio.instant();
                        Partida nova = new Partida(unidade.proximoId(), m, v, dataPartida, eventId);
                        nova.encerrar(resultado);
                        partidas.salvar(nova);

                        auditoria.registrar(TipoEventoAuditoria.REGISTRO_RESULTADO, adminId,
                                "Historico ESPN importado para Poisson: " + nova.descricao() + " ["
                                        + golsMandante + " x " + golsVisitante + ", cartoes: " + totalCartoes + "]");
                        return true;
                    });
                    if (importada) {
                        historicoCriado++;
                    }
                }
            }
        }

        String msg = "Sincronizacao de historico concluida. " + partidasApuradas + " partidas apuradas ("
                + apostasApuradas + " apostas), " + historicoCriado + " partidas de historico importadas.";
        auditoria.registrar(TipoEventoAuditoria.SINCRONIZACAO_ESPN, adminId, msg);

        return new ResultadoSincronizacaoHistorico(
                true, msg, total, partidasApuradas, apostasApuradas, historicoCriado, jaEncerradas, emAndamento);
    }

    public ResultadoSincronizacaoCompleta sincronizarTudo(Sessao sessao) {
        ResultadoSincronizacaoAgendadas agendadas = sincronizarProximosJogos(sessao);
        ResultadoSincronizacaoHistorico historico = sincronizarHistoricoEApuracao(sessao, null);
        return new ResultadoSincronizacaoCompleta(agendadas, historico);
    }

    public Equipe obterOuCriarEquipe(String nomeRaw) {
        String nome = NormalizadorEquipes.normalizar(nomeRaw);
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "Nome de equipe invalido");
        }
        return equipes.buscarPorNome(nome).orElseGet(() -> {
            Equipe nova = new Equipe(unidade.proximoId(), nome);
            equipes.salvar(nova);
            return nova;
        });
    }
}
