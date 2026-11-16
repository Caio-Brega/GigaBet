package br.ufmg.dcc603.apostas.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public abstract class Aposta {
    private final long id;
    private final long apostadorId;
    private final long partidaId;
    private final BigDecimal valor;
    private final BigDecimal probabilidade;
    private final BigDecimal premioPotencial;
    private final Instant criadaEm;
    private StatusAposta status;

    protected Aposta(
            long id,
            long apostadorId,
            long partidaId,
            BigDecimal valor,
            BigDecimal probabilidade,
            BigDecimal premioPotencial,
            Instant criadaEm) {
        this.id = id;
        this.apostadorId = apostadorId;
        this.partidaId = partidaId;
        this.valor = Objects.requireNonNull(valor);
        this.probabilidade = Objects.requireNonNull(probabilidade);
        this.premioPotencial = Objects.requireNonNull(premioPotencial);
        this.criadaEm = Objects.requireNonNull(criadaEm);
        this.status = StatusAposta.ABERTA;
    }

    protected Aposta(Aposta origem) {
        this.id = origem.id;
        this.apostadorId = origem.apostadorId;
        this.partidaId = origem.partidaId;
        this.valor = origem.valor;
        this.probabilidade = origem.probabilidade;
        this.premioPotencial = origem.premioPotencial;
        this.criadaEm = origem.criadaEm;
        this.status = origem.status;
    }

    public long id() {
        return id;
    }

    public long apostadorId() {
        return apostadorId;
    }

    public long partidaId() {
        return partidaId;
    }

    public BigDecimal valor() {
        return valor;
    }

    public BigDecimal probabilidade() {
        return probabilidade;
    }

    public BigDecimal premioPotencial() {
        return premioPotencial;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public StatusAposta status() {
        return status;
    }

    public abstract TipoPrevisao tipo();

    public abstract String descricaoPrevisao();

    public abstract boolean avaliar(ResultadoPartida resultado);

    public abstract Aposta copiar();

    public void marcarVencedora() {
        exigirAberta();
        status = StatusAposta.VENCEDORA;
    }

    public void marcarPerdedora() {
        exigirAberta();
        status = StatusAposta.PERDEDORA;
    }

    public void restaurarStatus(StatusAposta status) {
        this.status = Objects.requireNonNull(status);
    }

    private void exigirAberta() {
        if (status != StatusAposta.ABERTA) {
            throw new IllegalStateException("Aposta ja apurada");
        }
    }
}
