package br.ufmg.dcc603.apostas.dominio;

import java.time.Instant;
import java.util.Objects;

public final class Partida {
    private final long id;
    private final Equipe mandante;
    private final Equipe visitante;
    private final Instant inicio;
    private final String externalId;
    private StatusPartida status;
    private ResultadoPartida resultado;

    public Partida(long id, Equipe mandante, Equipe visitante, Instant inicio) {
        this(id, mandante, visitante, inicio, null);
    }

    public Partida(long id, Equipe mandante, Equipe visitante, Instant inicio, String externalId) {
        this.id = id;
        this.mandante = Objects.requireNonNull(mandante);
        this.visitante = Objects.requireNonNull(visitante);
        this.inicio = Objects.requireNonNull(inicio);
        this.externalId = externalId != null && !externalId.isBlank() ? externalId.trim() : null;
        this.status = StatusPartida.AGENDADA;
    }

    private Partida(Partida origem) {
        this.id = origem.id;
        this.mandante = origem.mandante;
        this.visitante = origem.visitante;
        this.inicio = origem.inicio;
        this.externalId = origem.externalId;
        this.status = origem.status;
        this.resultado = origem.resultado;
    }

    public long id() {
        return id;
    }

    public String externalId() {
        return externalId;
    }

    public Equipe mandante() {
        return mandante;
    }

    public Equipe visitante() {
        return visitante;
    }

    public Instant inicio() {
        return inicio;
    }

    public StatusPartida status() {
        return status;
    }

    public ResultadoPartida resultado() {
        return resultado;
    }

    public String descricao() {
        return mandante.nome() + " x " + visitante.nome();
    }

    public boolean mesmasEquipes(Partida outra) {
        long a = mandante.id();
        long b = visitante.id();
        long c = outra.mandante.id();
        long d = outra.visitante.id();
        return (a == c && b == d) || (a == d && b == c);
    }

    public boolean aceitaApostas(Instant agora) {
        return status == StatusPartida.AGENDADA && inicio.isAfter(agora);
    }

    public void marcarEmAndamentoSeIniciou(Instant agora) {
        if (status == StatusPartida.AGENDADA && !inicio.isAfter(agora)) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    public void marcarEmAndamento() {
        if (status == StatusPartida.AGENDADA) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    public void encerrar(ResultadoPartida resultadoPartida) {
        if (status == StatusPartida.ENCERRADA) {
            throw new IllegalStateException("Resultado imutavel");
        }
        this.resultado = Objects.requireNonNull(resultadoPartida);
        this.status = StatusPartida.ENCERRADA;
    }

    public void restaurar(StatusPartida status, ResultadoPartida resultado) {
        this.status = Objects.requireNonNull(status);
        this.resultado = resultado;
    }

    public Partida copiar() {
        return new Partida(this);
    }
}
