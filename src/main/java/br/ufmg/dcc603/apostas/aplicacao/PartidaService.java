package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Administrador;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class PartidaService {
    private final EquipeRepository equipes;
    private final PartidaRepository partidas;
    private final UnidadeDeTrabalho unidade;
    private final AutenticacaoService autenticacao;
    private final ApuracaoService apuracao;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public PartidaService(
            EquipeRepository equipes,
            PartidaRepository partidas,
            UnidadeDeTrabalho unidade,
            AutenticacaoService autenticacao,
            ApuracaoService apuracao,
            AuditoriaService auditoria,
            Clock relogio) {
        this.equipes = equipes;
        this.partidas = partidas;
        this.unidade = unidade;
        this.autenticacao = autenticacao;
        this.apuracao = apuracao;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public Equipe cadastrarEquipe(Sessao sessao, String nome) {
        Administrador administrador = autenticacao.exigirAdministrador(sessao);
        Equipe equipe = novaEquipe(nome);
        auditoria.registrar(TipoEventoAuditoria.CADASTRO_EQUIPE, administrador.id(),
                "Equipe cadastrada: " + equipe.nome());
        return equipe;
    }

    public Partida cadastrarPartida(Sessao sessao, String nomeMandante, String nomeVisitante, Instant inicio) {
        Administrador administrador = autenticacao.exigirAdministrador(sessao);
        String mandante = nomeObrigatorio(nomeMandante);
        String visitante = nomeObrigatorio(nomeVisitante);
        if (mandante.equalsIgnoreCase(visitante)) {
            throw new RegraNegocioException(CodigoRegra.EQUIPES_IGUAIS);
        }
        if (inicio == null || !inicio.isAfter(relogio.instant())) {
            throw new RegraNegocioException(CodigoRegra.DATA_NO_PASSADO);
        }
        Equipe casa = obterOuCriar(mandante);
        Equipe fora = obterOuCriar(visitante);
        if (casa.id() == fora.id()) {
            throw new RegraNegocioException(CodigoRegra.EQUIPES_IGUAIS);
        }
        Partida partida = new Partida(unidade.proximoId(), casa, fora, inicio);
        partidas.salvar(partida);
        auditoria.registrar(TipoEventoAuditoria.CADASTRO_PARTIDA, administrador.id(),
                "Partida cadastrada: " + partida.descricao());
        return partida;
    }

    public void sincronizarStatus() {
        Instant agora = relogio.instant();
        for (Partida partida : partidas.listar()) {
            StatusPartida antes = partida.status();
            partida.marcarEmAndamentoSeIniciou(agora);
            if (partida.status() != antes) {
                partidas.salvar(partida);
            }
        }
    }

    public List<Partida> listarCatalogo() {
        sincronizarStatus();
        return partidas.listar();
    }

    public List<Equipe> listarEquipes() {
        return equipes.listar().stream()
                .sorted(Comparator.comparing(Equipe::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void semearDemonstracaoSeVazio(Sessao admin) {
        autenticacao.exigirAdministrador(admin);
        if (!partidas.listar().isEmpty()) {
            return;
        }
        unidade.executar(() -> {
            if (!partidas.listar().isEmpty()) {
                return null;
            }
            autenticacao.cadastrar(new PedidoCadastro(
                    "Carlos Apostador", "39053344705", "carlos@email.com", "senha123", true));
            Sessao carlos = autenticacao.autenticar("carlos@email.com", "senha123");
            autenticacao.depositar(carlos, new BigDecimal("150.00"));
            Instant agora = relogio.instant();
            int[][] placares = {{2, 1}, {1, 0}, {3, 0}, {2, 0}, {0, 1}, {0, 2}, {1, 1}, {2, 2}};
            int[] cartoes = {3, 2, 5, 1, 4, 3, 2, 4};
            for (int i = 0; i < placares.length; i++) {
                gravarEncerrada("Flamengo", "Palmeiras", agora.minus(Duration.ofDays(10L * (i + 1))),
                        placares[i][0], placares[i][1], cartoes[i]);
            }
            gravarEmAndamento("Santos", "Vasco", agora.minus(Duration.ofHours(2)));
            cadastrarPartida(admin, "Flamengo", "Palmeiras", agora.plus(Duration.ofHours(24)));
            cadastrarPartida(admin, "Corinthians", "São Paulo", agora.plus(Duration.ofHours(48)));
            cadastrarPartida(admin, "Cruzeiro", "Atlético-MG", agora.plus(Duration.ofHours(72)));
            cadastrarPartida(admin, "Grêmio", "Internacional", agora.plus(Duration.ofHours(96)));
            return null;
        });
    }

    public List<Partida> listarDisponiveis(Sessao sessao) {
        autenticacao.exigirApostador(sessao);
        sincronizarStatus();
        Instant agora = relogio.instant();
        return partidas.listar().stream().filter(partida -> partida.aceitaApostas(agora)).toList();
    }

    public List<Partida> listarTodas(Sessao sessao) {
        autenticacao.exigir(sessao);
        sincronizarStatus();
        return partidas.listar();
    }

    public Partida detalhar(Sessao sessao, long partidaId) {
        autenticacao.exigir(sessao);
        sincronizarStatus();
        return buscar(partidaId);
    }

    public ResumoApuracao registrarResultado(
            Sessao sessao, long partidaId, int golsMandante, int golsVisitante, int cartoesAmarelos) {
        Administrador administrador = autenticacao.exigirAdministrador(sessao);
        if (golsMandante < 0 || golsVisitante < 0 || cartoesAmarelos < 0) {
            throw new RegraNegocioException(CodigoRegra.RESULTADO_INVALIDO);
        }
        sincronizarStatus();
        Partida partida = buscar(partidaId);
        if (partida.status() == StatusPartida.ENCERRADA) {
            throw new RegraNegocioException(CodigoRegra.PARTIDA_JA_ENCERRADA);
        }
        if (partida.status() == StatusPartida.AGENDADA) {
            throw new RegraNegocioException(CodigoRegra.PARTIDA_AINDA_NAO_INICIADA);
        }
        ResultadoPartida resultado = new ResultadoPartida(golsMandante, golsVisitante, cartoesAmarelos);
        return unidade.executar(() -> {
            partida.encerrar(resultado);
            partidas.salvar(partida);
            ResumoApuracao resumo = apuracao.apurar(partida);
            auditoria.registrar(TipoEventoAuditoria.REGISTRO_RESULTADO, administrador.id(),
                    "Resultado de " + partida.descricao() + ": " + golsMandante + " x " + golsVisitante
                            + ", cartoes " + cartoesAmarelos);
            auditoria.registrar(TipoEventoAuditoria.APURACAO, administrador.id(),
                    "Apuracao de " + partida.descricao() + ": " + resumo.apostasApuradas()
                            + " apostas, " + resumo.vencedoras() + " vencedoras, "
                            + resumo.perdedoras() + " perdedoras");
            return resumo;
        });
    }

    public Partida buscar(long partidaId) {
        return partidas.buscarPorId(partidaId)
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.PARTIDA_NAO_ENCONTRADA));
    }

    private void gravarEncerrada(
            String nomeMandante, String nomeVisitante, Instant inicio, int golsMandante, int golsVisitante, int cartoes) {
        Partida partida = novaPartida(nomeMandante, nomeVisitante, inicio);
        partida.encerrar(new ResultadoPartida(golsMandante, golsVisitante, cartoes));
        partidas.salvar(partida);
    }

    private void gravarEmAndamento(String nomeMandante, String nomeVisitante, Instant inicio) {
        Partida partida = novaPartida(nomeMandante, nomeVisitante, inicio);
        partida.marcarEmAndamentoSeIniciou(inicio.plusSeconds(60));
        partidas.salvar(partida);
    }

    private Partida novaPartida(String nomeMandante, String nomeVisitante, Instant inicio) {
        Equipe casa = obterOuCriar(nomeMandante);
        Equipe fora = obterOuCriar(nomeVisitante);
        Partida partida = new Partida(unidade.proximoId(), casa, fora, inicio);
        partidas.salvar(partida);
        return partida;
    }

    private Equipe obterOuCriar(String nome) {
        return equipes.buscarPorNome(nome).orElseGet(() -> novaEquipe(nome));
    }

    private Equipe novaEquipe(String nome) {
        String limpo = nomeObrigatorio(nome);
        if (equipes.buscarPorNome(limpo).isPresent()) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "Equipe ja cadastrada.");
        }
        Equipe equipe = new Equipe(unidade.proximoId(), limpo);
        equipes.salvar(equipe);
        return equipe;
    }

    private String nomeObrigatorio(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "Nome da equipe obrigatorio.");
        }
        return nome.trim();
    }
}
