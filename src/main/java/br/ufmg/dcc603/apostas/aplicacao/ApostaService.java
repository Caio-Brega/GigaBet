package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.ApostaCartoesAmarelos;
import br.ufmg.dcc603.apostas.dominio.ApostaEquipeVencedora;
import br.ufmg.dcc603.apostas.dominio.ApostaPlacarExato;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;

public final class ApostaService {
    public static final int LIMITE_ABERTAS = 10;

    private final ApostaRepository apostas;
    private final PartidaRepository partidas;
    private final UsuarioRepository usuarios;
    private final UnidadeDeTrabalho unidade;
    private final AutenticacaoService autenticacao;
    private final PartidaService partidaService;
    private final CalculadoraProbabilidade probabilidade;
    private final CalculadoraPremio premio;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public ApostaService(
            ApostaRepository apostas,
            PartidaRepository partidas,
            UsuarioRepository usuarios,
            UnidadeDeTrabalho unidade,
            AutenticacaoService autenticacao,
            PartidaService partidaService,
            CalculadoraProbabilidade probabilidade,
            CalculadoraPremio premio,
            AuditoriaService auditoria,
            Clock relogio) {
        this.apostas = apostas;
        this.partidas = partidas;
        this.usuarios = usuarios;
        this.unidade = unidade;
        this.autenticacao = autenticacao;
        this.partidaService = partidaService;
        this.probabilidade = probabilidade;
        this.premio = premio;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public CotacaoAposta cotar(Sessao sessao, PedidoAposta pedido) {
        Apostador apostador = autenticacao.exigirApostador(sessao);
        Partida partida = partidaPronta(pedido);
        BigDecimal valor = validar(apostador, partida, pedido);
        Estimativa estimativa = probabilidade.estimar(partida, pedido, partidas.listar());
        return new CotacaoAposta(estimativa.probabilidade(), premio.calcular(valor, estimativa), apostador.saldo());
    }

    public ComprovanteAposta confirmar(Sessao sessao, PedidoAposta pedido) {
        Apostador apostador = autenticacao.exigirApostador(sessao);
        partidaService.sincronizarStatus();
        return unidade.executar(() -> {
            Partida partida = partidaAtual(pedido.partidaId());
            BigDecimal valor = validar(apostador, partida, pedido);
            Estimativa estimativa = probabilidade.estimar(partida, pedido, partidas.listar());
            BigDecimal premioPotencial = premio.calcular(valor, estimativa);
            apostador.debitar(valor);
            usuarios.salvar(apostador);
            Aposta aposta = novaAposta(unidade.proximoId(), apostador.id(), partida.id(), valor,
                    estimativa.probabilidade(), premioPotencial, pedido);
            apostas.salvar(aposta);
            auditoria.registrar(TipoEventoAuditoria.REGISTRO_APOSTA, apostador.id(),
                    "Aposta " + aposta.id() + " em " + partida.descricao() + ": " + aposta.descricaoPrevisao());
            return new ComprovanteAposta(
                    aposta.id(),
                    partida.id(),
                    partida.descricao(),
                    aposta.tipo(),
                    aposta.descricaoPrevisao(),
                    aposta.valor(),
                    aposta.probabilidade(),
                    aposta.premioPotencial(),
                    aposta.status(),
                    aposta.criadaEm(),
                    apostador.saldo());
        });
    }

    public List<ItemHistorico> consultarHistorico(Sessao sessao, FiltroHistorico filtro) {
        Apostador apostador = autenticacao.exigirApostador(sessao);
        FiltroHistorico aplicado = filtro == null ? FiltroHistorico.nenhum() : filtro;
        return apostas.listarPorApostador(apostador.id()).stream()
                .filter(aposta -> aplicado.status() == null || aposta.status() == aplicado.status())
                .filter(aposta -> aplicado.de() == null || !aposta.criadaEm().isBefore(aplicado.de()))
                .filter(aposta -> aplicado.ate() == null || !aposta.criadaEm().isAfter(aplicado.ate()))
                .sorted(Comparator.comparing(Aposta::criadaEm).reversed().thenComparing(Aposta::id))
                .map(aposta -> {
                    Partida partida = partidaService.buscar(aposta.partidaId());
                    return new ItemHistorico(
                            aposta.id(),
                            partida.id(),
                            partida.descricao(),
                            aposta.tipo(),
                            aposta.descricaoPrevisao(),
                            aposta.valor(),
                            aposta.probabilidade(),
                            aposta.premioPotencial(),
                            aposta.status(),
                            aposta.criadaEm(),
                            partida.resultado());
                })
                .toList();
    }

    private Partida partidaPronta(PedidoAposta pedido) {
        partidaService.sincronizarStatus();
        return partidaAtual(pedido == null ? 0 : pedido.partidaId());
    }

    private Partida partidaAtual(long partidaId) {
        return partidas.buscarPorId(partidaId)
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.PARTIDA_NAO_ENCONTRADA));
    }

    private BigDecimal validar(Apostador apostador, Partida partida, PedidoAposta pedido) {
        if (pedido == null) {
            throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA);
        }
        if (!partida.aceitaApostas(relogio.instant())) {
            throw new RegraNegocioException(CodigoRegra.PARTIDA_INDISPONIVEL);
        }
        if (apostas.contarAbertas(apostador.id()) >= LIMITE_ABERTAS) {
            throw new RegraNegocioException(CodigoRegra.LIMITE_APOSTAS_ABERTAS);
        }
        if (apostas.existeAberta(apostador.id(), partida.id())) {
            throw new RegraNegocioException(CodigoRegra.APOSTA_DUPLICADA_NA_PARTIDA);
        }
        BigDecimal valor = Dinheiro.positivo(pedido.valor());
        if (valor.compareTo(apostador.saldo()) > 0) {
            throw new RegraNegocioException(CodigoRegra.SALDO_INSUFICIENTE,
                    "Saldo insuficiente. Disponivel: " + apostador.saldo());
        }
        validarPrevisao(pedido);
        return valor;
    }

    private void validarPrevisao(PedidoAposta pedido) {
        if (pedido.tipo() == null) {
            throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA);
        }
        switch (pedido.tipo()) {
            case EQUIPE_VENCEDORA -> {
                if (pedido.desfecho() == null) {
                    throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA);
                }
            }
            case PLACAR_EXATO -> {
                Integer mandante = pedido.golsMandante();
                Integer visitante = pedido.golsVisitante();
                if (mandante == null || visitante == null
                        || mandante < 0 || mandante > 9
                        || visitante < 0 || visitante > 9) {
                    throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA,
                            "O placar exato aceita de 0 a 9 gols para cada equipe.");
                }
            }
            case CARTOES_AMARELOS -> {
                Integer cartoes = pedido.cartoesAmarelos();
                if (cartoes == null || cartoes < 0 || cartoes > 10) {
                    throw new RegraNegocioException(CodigoRegra.PREVISAO_INVALIDA,
                            "O total de cartoes amarelos deve estar entre 0 e 10.");
                }
            }
        }
    }

    private Aposta novaAposta(
            long id,
            long apostadorId,
            long partidaId,
            BigDecimal valor,
            BigDecimal probabilidadeEstimada,
            BigDecimal premioPotencial,
            PedidoAposta pedido) {
        return switch (pedido.tipo()) {
            case EQUIPE_VENCEDORA -> new ApostaEquipeVencedora(
                    id, apostadorId, partidaId, valor, probabilidadeEstimada, premioPotencial,
                    relogio.instant(), pedido.desfecho());
            case PLACAR_EXATO -> new ApostaPlacarExato(
                    id, apostadorId, partidaId, valor, probabilidadeEstimada, premioPotencial,
                    relogio.instant(), pedido.golsMandante(), pedido.golsVisitante());
            case CARTOES_AMARELOS -> new ApostaCartoesAmarelos(
                    id, apostadorId, partidaId, valor, probabilidadeEstimada, premioPotencial,
                    relogio.instant(), pedido.cartoesAmarelos());
        };
    }
}
