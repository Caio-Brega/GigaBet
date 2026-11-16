package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.dominio.Administrador;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Perfil;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class UsuarioRepositoryJdbc implements UsuarioRepository {
    private static final String COLUNAS = """
            id, perfil, nome, email, senha_hash, tentativas_falhas, bloqueada_ate,
            cpf_cifrado, consentimento, saldo
            """;

    private final Banco banco;
    private final CpfCofre cpf;

    public UsuarioRepositoryJdbc(Banco banco, CpfCofre cpf) {
        this.banco = banco;
        this.cpf = cpf;
    }

    @Override
    public void salvar(Usuario usuario) {
        String cifrado = null;
        String resumo = null;
        Boolean consentimento = null;
        BigDecimal saldo = null;
        if (usuario instanceof Apostador apostador) {
            cifrado = cpf.cifrar(apostador.cpf());
            resumo = cpf.resumo(apostador.cpf());
            consentimento = apostador.consentimento();
            saldo = apostador.saldo();
        }
        banco.atualizar("""
                INSERT INTO usuarios (
                    id, perfil, nome, email, senha_hash, tentativas_falhas, bloqueada_ate,
                    cpf_cifrado, cpf_resumo, consentimento, saldo
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    perfil = EXCLUDED.perfil,
                    nome = EXCLUDED.nome,
                    email = EXCLUDED.email,
                    senha_hash = EXCLUDED.senha_hash,
                    tentativas_falhas = EXCLUDED.tentativas_falhas,
                    bloqueada_ate = EXCLUDED.bloqueada_ate,
                    cpf_cifrado = EXCLUDED.cpf_cifrado,
                    cpf_resumo = EXCLUDED.cpf_resumo,
                    consentimento = EXCLUDED.consentimento,
                    saldo = EXCLUDED.saldo
                """,
                usuario.id(),
                usuario.perfil(),
                usuario.nome(),
                usuario.email(),
                usuario.senhaHash(),
                usuario.tentativasFalhas(),
                usuario.bloqueadaAte(),
                cifrado,
                resumo,
                consentimento,
                saldo);
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return banco.consultarUm("SELECT " + COLUNAS + " FROM usuarios WHERE id = ?", this::mapear, id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return banco.consultarUm(
                "SELECT " + COLUNAS + " FROM usuarios WHERE lower(email) = lower(?)",
                this::mapear, email.trim());
    }

    @Override
    public Optional<Usuario> buscarPorCpf(String cpfInformado) {
        if (cpfInformado == null) {
            return Optional.empty();
        }
        return banco.consultarUm(
                "SELECT " + COLUNAS + " FROM usuarios WHERE cpf_resumo = ?",
                this::mapear, cpf.resumo(cpfInformado));
    }

    @Override
    public List<Usuario> listar() {
        return banco.listar("SELECT " + COLUNAS + " FROM usuarios ORDER BY id", this::mapear);
    }

    private Usuario mapear(ResultSet linhas) throws SQLException {
        long id = linhas.getLong("id");
        Perfil perfil = Perfil.valueOf(linhas.getString("perfil"));
        String nome = linhas.getString("nome");
        String email = linhas.getString("email");
        String senha = linhas.getString("senha_hash");
        Usuario usuario;
        if (perfil == Perfil.ADMINISTRADOR) {
            usuario = new Administrador(id, nome, email, senha);
        } else {
            String documento = cpf.decifrar(linhas.getString("cpf_cifrado"));
            boolean consentimento = linhas.getBoolean("consentimento");
            Apostador apostador = new Apostador(id, nome, email, senha, documento, consentimento);
            BigDecimal saldo = linhas.getBigDecimal("saldo");
            apostador.restaurarSaldo(saldo == null ? new BigDecimal("0.00") : saldo);
            usuario = apostador;
        }
        Timestamp bloqueio = linhas.getTimestamp("bloqueada_ate");
        Instant ate = bloqueio == null ? null : bloqueio.toInstant();
        usuario.restaurarAcesso(linhas.getInt("tentativas_falhas"), ate);
        return usuario;
    }
}
