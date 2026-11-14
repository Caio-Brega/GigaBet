package br.ufmg.dcc603.apostas;

import br.ufmg.dcc603.apostas.aplicacao.AutenticacaoService;
import br.ufmg.dcc603.apostas.aplicacao.PedidoCadastro;
import br.ufmg.dcc603.apostas.aplicacao.Sessao;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.tempo.RelogioMutavel;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public final class Suporte {
    public final RelogioMutavel relogio;
    public final Sistema sistema;
    public final Sessao admin;

    public Suporte() {
        this(Instant.parse("2026-06-01T15:00:00Z"));
    }

    public Suporte(Instant inicio) {
        relogio = new RelogioMutavel(inicio);
        sistema = Sistema.criar(relogio);
        admin = sistema.autenticacao().autenticar(
                AutenticacaoService.EMAIL_ADMIN, AutenticacaoService.SENHA_ADMIN);
    }

    public Sessao apostador(String nome, String email, String cpf, String saldo) {
        sistema.autenticacao().cadastrar(new PedidoCadastro(nome, cpf, email, "senha123", true));
        Sessao sessao = sistema.autenticacao().autenticar(email, "senha123");
        if (saldo != null) {
            sistema.autenticacao().depositar(sessao, new BigDecimal(saldo));
        }
        return sessao;
    }

    public Partida agendar(String mandante, String visitante, Duration daquiA) {
        return sistema.partidas().cadastrarPartida(
                admin, mandante, visitante, relogio.instant().plus(daquiA));
    }

    public void encerrar(Partida partida, int golsMandante, int golsVisitante, int cartoes) {
        relogio.ajustar(partida.inicio().plusSeconds(60));
        sistema.partidas().registrarResultado(admin, partida.id(), golsMandante, golsVisitante, cartoes);
    }
}
