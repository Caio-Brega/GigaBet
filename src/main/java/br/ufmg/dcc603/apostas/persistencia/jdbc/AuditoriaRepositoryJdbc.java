package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.persistencia.AuditoriaRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class AuditoriaRepositoryJdbc implements AuditoriaRepository {
    private final Banco banco;

    public AuditoriaRepositoryJdbc(Banco banco) {
        this.banco = banco;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        banco.atualizar("""
                INSERT INTO auditoria (id, quando, tipo, usuario_id, descricao)
                VALUES (?, ?, ?, ?, ?)
                """,
                registro.id(),
                registro.quando(),
                registro.tipo(),
                registro.usuarioId(),
                registro.descricao());
    }

    @Override
    public List<RegistroAuditoria> listar() {
        return banco.listar("""
                SELECT id, quando, tipo, usuario_id, descricao
                FROM auditoria ORDER BY id
                """, this::mapear);
    }

    private RegistroAuditoria mapear(ResultSet linhas) throws SQLException {
        long usuario = linhas.getLong("usuario_id");
        Long usuarioId = linhas.wasNull() ? null : usuario;
        return new RegistroAuditoria(
                linhas.getLong("id"),
                linhas.getTimestamp("quando").toInstant(),
                TipoEventoAuditoria.valueOf(linhas.getString("tipo")),
                usuarioId,
                linhas.getString("descricao"));
    }
}
