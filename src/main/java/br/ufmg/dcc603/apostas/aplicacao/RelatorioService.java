package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RelatorioService {
    private final AutenticacaoService autenticacao;
    private final ApostaRepository apostas;
    private final PartidaRepository partidas;
    private final UsuarioRepository usuarios;

    public RelatorioService(
            AutenticacaoService autenticacao,
            ApostaRepository apostas,
            PartidaRepository partidas,
            UsuarioRepository usuarios) {
        this.autenticacao = autenticacao;
        this.apostas = apostas;
        this.partidas = partidas;
        this.usuarios = usuarios;
    }

    public RelatorioPartida porPartida(Sessao sessao, long partidaId) {
        autenticacao.exigirAdministrador(sessao);
        Partida partida = partidas.buscarPorId(partidaId)
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.PARTIDA_NAO_ENCONTRADA));
        List<Aposta> daPartida = apostas.listarPorPartida(partidaId);
        return new RelatorioPartida(
                partida.id(),
                partida.descricao(),
                daPartida.size(),
                somar(daPartida, false),
                contarPorTipo(daPartida),
                contarPorStatus(daPartida),
                somar(daPartida, true),
                daPartida.stream().map(aposta -> linha(aposta, partida)).toList());
    }

    public RelatorioApostador porApostador(Sessao sessao, String nomeOuCpf) {
        autenticacao.exigirAdministrador(sessao);
        Apostador apostador = localizar(nomeOuCpf);
        List<Aposta> doApostador = apostas.listarPorApostador(apostador.id());
        Map<Long, List<LinhaRelatorio>> porPartida = new LinkedHashMap<>();
        for (Aposta aposta : doApostador) {
            Partida partida = partidas.buscarPorId(aposta.partidaId())
                    .orElseThrow(() -> new RegraNegocioException(CodigoRegra.PARTIDA_NAO_ENCONTRADA));
            porPartida.computeIfAbsent(partida.id(), id -> new ArrayList<>()).add(linha(aposta, partida));
        }
        return new RelatorioApostador(
                apostador.id(),
                apostador.nome(),
                apostador.cpf(),
                doApostador.size(),
                somar(doApostador, false),
                contar(doApostador, StatusAposta.ABERTA),
                contar(doApostador, StatusAposta.VENCEDORA),
                contar(doApostador, StatusAposta.PERDEDORA),
                somar(doApostador, true),
                porPartida);
    }

    private Apostador localizar(String nomeOuCpf) {
        if (nomeOuCpf == null || nomeOuCpf.isBlank()) {
            throw new RegraNegocioException(CodigoRegra.APOSTADOR_NAO_ENCONTRADO);
        }
        String texto = nomeOuCpf.trim();
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() == 11) {
            return usuarios.buscarPorCpf(digitos)
                    .filter(Apostador.class::isInstance)
                    .map(Apostador.class::cast)
                    .orElseThrow(() -> new RegraNegocioException(CodigoRegra.APOSTADOR_NAO_ENCONTRADO));
        }
        return usuarios.listar().stream()
                .filter(Apostador.class::isInstance)
                .map(Apostador.class::cast)
                .filter(apostador -> apostador.nome().equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.APOSTADOR_NAO_ENCONTRADO));
    }

    private LinhaRelatorio linha(Aposta aposta, Partida partida) {
        Usuario usuario = usuarios.buscarPorId(aposta.apostadorId()).orElseThrow();
        return new LinhaRelatorio(
                aposta.id(),
                partida.id(),
                partida.descricao(),
                usuario.nome(),
                aposta.tipo(),
                aposta.descricaoPrevisao(),
                aposta.valor(),
                aposta.premioPotencial(),
                aposta.status());
    }

    private BigDecimal somar(List<Aposta> lista, boolean somentePremiosPagos) {
        BigDecimal total = Dinheiro.zero();
        for (Aposta aposta : lista) {
            if (somentePremiosPagos) {
                if (aposta.status() == StatusAposta.VENCEDORA) {
                    total = total.add(aposta.premioPotencial());
                }
            } else {
                total = total.add(aposta.valor());
            }
        }
        return total;
    }

    private Map<TipoPrevisao, Integer> contarPorTipo(List<Aposta> lista) {
        Map<TipoPrevisao, Integer> totais = new EnumMap<>(TipoPrevisao.class);
        for (TipoPrevisao tipo : TipoPrevisao.values()) {
            totais.put(tipo, 0);
        }
        for (Aposta aposta : lista) {
            totais.merge(aposta.tipo(), 1, Integer::sum);
        }
        return totais;
    }

    private Map<StatusAposta, Integer> contarPorStatus(List<Aposta> lista) {
        Map<StatusAposta, Integer> totais = new EnumMap<>(StatusAposta.class);
        for (StatusAposta status : StatusAposta.values()) {
            totais.put(status, 0);
        }
        for (Aposta aposta : lista) {
            totais.merge(aposta.status(), 1, Integer::sum);
        }
        return totais;
    }

    private int contar(List<Aposta> lista, StatusAposta status) {
        return (int) lista.stream().filter(aposta -> aposta.status() == status).count();
    }
}
