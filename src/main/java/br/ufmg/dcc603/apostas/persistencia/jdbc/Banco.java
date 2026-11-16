package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class Banco implements UnidadeDeTrabalho {
    private static final String[] ESQUEMA = {
            "CREATE SEQUENCE IF NOT EXISTS ids AS BIGINT START WITH 1",
            """
            CREATE TABLE IF NOT EXISTS equipes (
                id BIGINT PRIMARY KEY,
                nome TEXT NOT NULL
            )
            """,
            "CREATE UNIQUE INDEX IF NOT EXISTS equipes_nome_unico ON equipes (lower(nome))",
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id BIGINT PRIMARY KEY,
                perfil TEXT NOT NULL,
                nome TEXT NOT NULL,
                email TEXT NOT NULL,
                senha_hash TEXT NOT NULL,
                tentativas_falhas INTEGER NOT NULL,
                bloqueada_ate TIMESTAMPTZ,
                cpf_cifrado TEXT,
                cpf_resumo TEXT,
                consentimento BOOLEAN,
                saldo NUMERIC(14,2)
            )
            """,
            "CREATE UNIQUE INDEX IF NOT EXISTS usuarios_email_unico ON usuarios (lower(email))",
            "CREATE UNIQUE INDEX IF NOT EXISTS usuarios_cpf_unico ON usuarios (cpf_resumo) WHERE cpf_resumo IS NOT NULL",
            """
            CREATE TABLE IF NOT EXISTS partidas (
                id BIGINT PRIMARY KEY,
                mandante_id BIGINT NOT NULL REFERENCES equipes (id),
                visitante_id BIGINT NOT NULL REFERENCES equipes (id),
                inicio TIMESTAMPTZ NOT NULL,
                status TEXT NOT NULL,
                gols_mandante INTEGER,
                gols_visitante INTEGER,
                cartoes_amarelos INTEGER
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS apostas (
                id BIGINT PRIMARY KEY,
                apostador_id BIGINT NOT NULL REFERENCES usuarios (id),
                partida_id BIGINT NOT NULL REFERENCES partidas (id),
                tipo TEXT NOT NULL,
                valor NUMERIC(14,2) NOT NULL,
                probabilidade NUMERIC(16,8) NOT NULL,
                premio_potencial NUMERIC(14,2) NOT NULL,
                criada_em TIMESTAMPTZ NOT NULL,
                status TEXT NOT NULL,
                desfecho TEXT,
                gols_mandante INTEGER,
                gols_visitante INTEGER,
                cartoes INTEGER
            )
            """,
            "ALTER TABLE apostas ALTER COLUMN probabilidade TYPE NUMERIC(24,12)",
            "CREATE INDEX IF NOT EXISTS apostas_apostador ON apostas (apostador_id)",
            "CREATE INDEX IF NOT EXISTS apostas_partida ON apostas (partida_id)",
            """
            CREATE TABLE IF NOT EXISTS auditoria (
                id BIGINT PRIMARY KEY,
                quando TIMESTAMPTZ NOT NULL,
                tipo TEXT NOT NULL,
                usuario_id BIGINT,
                descricao TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS sessoes (
                token TEXT PRIMARY KEY,
                usuario_id BIGINT NOT NULL REFERENCES usuarios (id),
                perfil TEXT NOT NULL
            )
            """
    };

    private final String url;
    private final String usuario;
    private final String senha;
    private final ThreadLocal<Connection> transacao = new ThreadLocal<>();

    public Banco(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver do Postgres ausente", e);
        }
    }

    public void instalarEsquema() {
        try {
            for (String sql : ESQUEMA) {
                atualizar(sql);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Nao foi possivel preparar o Postgres em " + url
                            + ". Suba o banco com docker compose up.", e);
        }
    }

    public void atualizar(String sql, Object... params) {
        Connection conexao = null;
        try {
            conexao = emprestar();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                vincular(comando, params);
                comando.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            devolver(conexao);
        }
    }

    public <T> Optional<T> consultarUm(String sql, Leitor<T> leitor, Object... params) {
        List<T> linhas = listar(sql, leitor, params);
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(linhas.get(0));
    }

    public <T> List<T> listar(String sql, Leitor<T> leitor, Object... params) {
        Connection conexao = null;
        try {
            conexao = emprestar();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                vincular(comando, params);
                try (ResultSet linhas = comando.executeQuery()) {
                    List<T> resultado = new ArrayList<>();
                    while (linhas.next()) {
                        resultado.add(leitor.ler(linhas));
                    }
                    return resultado;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            devolver(conexao);
        }
    }

    @Override
    public long proximoId() {
        return consultarUm("SELECT nextval('ids')", linhas -> linhas.getLong(1))
                .orElseThrow(() -> new IllegalStateException("Sequencia ids indisponivel"));
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        if (transacao.get() != null) {
            return acao.get();
        }
        try (Connection conexao = abrir()) {
            conexao.setAutoCommit(false);
            transacao.set(conexao);
            try {
                T resultado = acao.get();
                conexao.commit();
                return resultado;
            } catch (RuntimeException e) {
                try {
                    conexao.rollback();
                } catch (SQLException falha) {
                    e.addSuppressed(falha);
                }
                throw e;
            } finally {
                transacao.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Connection emprestar() throws SQLException {
        Connection atual = transacao.get();
        if (atual != null) {
            return atual;
        }
        return abrir();
    }

    private void devolver(Connection conexao) {
        if (conexao != null && conexao != transacao.get()) {
            try {
                conexao.close();
            } catch (SQLException ignored) {
                // a operacao que falhou ja carregou a causa
            }
        }
    }

    private Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    private static void vincular(PreparedStatement comando, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object valor = params[i];
            int indice = i + 1;
            if (valor == null) {
                comando.setNull(indice, Types.NULL);
            } else if (valor instanceof Instant instante) {
                comando.setTimestamp(indice, Timestamp.from(instante));
            } else if (valor instanceof BigDecimal numero) {
                comando.setBigDecimal(indice, numero);
            } else if (valor instanceof Integer numero) {
                comando.setInt(indice, numero);
            } else if (valor instanceof Long numero) {
                comando.setLong(indice, numero);
            } else if (valor instanceof Boolean booleano) {
                comando.setBoolean(indice, booleano);
            } else if (valor instanceof Enum<?> enumerado) {
                comando.setString(indice, enumerado.name());
            } else {
                comando.setString(indice, valor.toString());
            }
        }
    }

    @FunctionalInterface
    public interface Leitor<T> {
        T ler(ResultSet linhas) throws SQLException;
    }
}
