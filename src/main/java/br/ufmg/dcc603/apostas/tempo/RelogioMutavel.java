package br.ufmg.dcc603.apostas.tempo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class RelogioMutavel extends Clock {
    private final ZoneId zona;
    private Instant instante;

    public RelogioMutavel(Instant instante) {
        this.instante = instante;
        this.zona = ZoneOffset.UTC;
    }

    public void ajustar(Instant instante) {
        this.instante = instante;
    }

    public void avancar(Duration duracao) {
        instante = instante.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
