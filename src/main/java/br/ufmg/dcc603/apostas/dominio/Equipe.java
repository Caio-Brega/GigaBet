package br.ufmg.dcc603.apostas.dominio;

import java.util.Objects;

public final class Equipe {
    private final long id;
    private final String nome;

    public Equipe(long id, String nome) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome);
    }

    public long id() {
        return id;
    }

    public String nome() {
        return nome;
    }
}
