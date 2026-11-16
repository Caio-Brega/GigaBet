package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.Sistema;
import br.ufmg.dcc603.apostas.aplicacao.FiltroHistorico;
import br.ufmg.dcc603.apostas.aplicacao.PedidoAposta;
import br.ufmg.dcc603.apostas.aplicacao.PedidoCadastro;
import br.ufmg.dcc603.apostas.aplicacao.Sessao;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PersistenciaJdbcTest {
    private static final String URL_BASE = "jdbc:postgresql://localhost:5434/gigabet";
    private static final String URL = "jdbc:postgresql://localhost:5434/gigabet_test";
    private static final String USUARIO = "gigabet";
    private static final String SENHA = "gigabet";
    private static final String CHAVE = "chave-de-teste";

    private static boolean disponivel;

    @BeforeAll
    static void prepararBanco() {
        try (Connection conexao = DriverManager.getConnection(URL_BASE, USUARIO, SENHA)) {
            conexao.setAutoCommit(true);
            boolean existe;
            try (PreparedStatement consulta = conexao.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = 'gigabet_test'");
                 ResultSet linhas = consulta.executeQuery()) {
                existe = linhas.next();
            }
            if (!existe) {
                try (Statement comando = conexao.createStatement()) {
                    comando.execute("CREATE DATABASE gigabet_test");
                }
            }
            disponivel = true;
        } catch (SQLException e) {
            disponivel = false;
        }
    }

    @Test
    void cifraEDecifraOCpfSemDeixarOTextoOriginal() {
        CpfCofre cofre = new CpfCofre("chave-de-teste");
        String cifrado = cofre.cifrar("52998224725");
        assertFalse(cifrado.contains("52998224725"));
        assertEquals("52998224725", cofre.decifrar(cifrado));
        assertEquals(cofre.resumo("52998224725"), cofre.resumo("52998224725"));
    }

    @Test
    void apostaSaldoCpfESessaoSobrevivemAoReinicio() throws Exception {
        assumeTrue(disponivel, "Postgres em localhost:5434");
        Sistema.criarJdbc(Clock.systemUTC(), URL, USUARIO, SENHA, CHAVE);
        limpar();

        Clock relogio = Clock.systemUTC();
        Sistema sistema = Sistema.criarJdbc(relogio, URL, USUARIO, SENHA, CHAVE);
        Sessao admin = sistema.autenticacao().autenticar("admin@apostas.local", "admin123");
        sistema.autenticacao().cadastrar(
                new PedidoCadastro("Ana Lima", "52998224725", "ana.jdbc@email.com", "senha123", true));
        Sessao ana = sistema.autenticacao().autenticar("ana.jdbc@email.com", "senha123");
        sistema.autenticacao().depositar(ana, new BigDecimal("50.00"));
        Partida partida = sistema.partidas().cadastrarPartida(
                admin, "Flamengo", "Palmeiras", Instant.now().plus(Duration.ofDays(2)));
        sistema.apostas().confirmar(
                ana, PedidoAposta.vencedor(partida.id(), Desfecho.MANDANTE, new BigDecimal("10.00")));

        Sistema deNovo = Sistema.criarJdbc(relogio, URL, USUARIO, SENHA, CHAVE);
        assertNotNull(deNovo.autenticacao().localizar(ana.token()));
        Sessao anaDeNovo = deNovo.autenticacao().autenticar("ana.jdbc@email.com", "senha123");
        Apostador carregada = deNovo.autenticacao().exigirApostador(anaDeNovo);
        assertEquals("52998224725", carregada.cpf());
        assertEquals(0, carregada.saldo().compareTo(new BigDecimal("40.00")));
        assertEquals(1, deNovo.apostas().consultarHistorico(anaDeNovo, FiltroHistorico.nenhum()).size());
        Partida recarregada = deNovo.partidas().listarCatalogo().stream()
                .filter(item -> item.id() == partida.id())
                .findFirst()
                .orElseThrow();
        assertEquals(StatusPartida.AGENDADA, recarregada.status());

        try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
             PreparedStatement consulta = conexao.prepareStatement(
                     "SELECT cpf_cifrado FROM usuarios WHERE email = ?")) {
            consulta.setString(1, "ana.jdbc@email.com");
            try (ResultSet linhas = consulta.executeQuery()) {
                assertEquals(true, linhas.next());
                assertFalse(linhas.getString("cpf_cifrado").contains("52998224725"));
            }
        }
    }

    private static void limpar() throws SQLException {
        try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement comando = conexao.createStatement()) {
            comando.execute("TRUNCATE sessoes, auditoria, apostas, partidas, usuarios, equipes RESTART IDENTITY CASCADE");
            comando.execute("ALTER SEQUENCE ids RESTART WITH 1");
        }
    }
}
