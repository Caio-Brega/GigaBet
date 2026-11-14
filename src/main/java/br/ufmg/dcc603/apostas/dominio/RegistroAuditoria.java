package br.ufmg.dcc603.apostas.dominio;

import java.time.Instant;
import java.util.Objects;

public final class RegistroAuditoria {
    private final long id;
    private final Instant quando;
    private final TipoEventoAuditoria tipo;
    private final Long usuarioId;
    private final String descricao;

    public RegistroAuditoria(long id, Instant quando, TipoEventoAuditoria tipo, Long usuarioId, String descricao) {
        this.id = id;
        this.quando = Objects.requireNonNull(quando);
        this.tipo = Objects.requireNonNull(tipo);
        this.usuarioId = usuarioId;
        this.descricao = Objects.requireNonNull(descricao);
    }

    public long id() {
        return id;
    }

    public Instant quando() {
        return quando;
    }

    public TipoEventoAuditoria tipo() {
        return tipo;
    }

    public Long usuarioId() {
        return usuarioId;
    }

    public String descricao() {
        return descricao;
    }
}
