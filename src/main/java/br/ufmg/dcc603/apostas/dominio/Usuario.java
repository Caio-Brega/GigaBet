package br.ufmg.dcc603.apostas.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public abstract class Usuario {
    public static final int LIMITE_TENTATIVAS = 5;
    public static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    private final long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private int tentativasFalhas;
    private Instant bloqueadaAte;

    protected Usuario(long id, String nome, String email, String senhaHash) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome);
        this.email = Objects.requireNonNull(email);
        this.senhaHash = Objects.requireNonNull(senhaHash);
    }

    protected Usuario(Usuario origem) {
        this.id = origem.id;
        this.nome = origem.nome;
        this.email = origem.email;
        this.senhaHash = origem.senhaHash;
        this.tentativasFalhas = origem.tentativasFalhas;
        this.bloqueadaAte = origem.bloqueadaAte;
    }

    public long id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String senhaHash() {
        return senhaHash;
    }

    public int tentativasFalhas() {
        return tentativasFalhas;
    }

    public Instant bloqueadaAte() {
        return bloqueadaAte;
    }

    public abstract Perfil perfil();

    public abstract Usuario copiar();

    public boolean bloqueada(Instant agora) {
        return bloqueadaAte != null && agora.isBefore(bloqueadaAte);
    }

    public void registrarFalha(Instant agora) {
        tentativasFalhas++;
        if (tentativasFalhas >= LIMITE_TENTATIVAS) {
            bloqueadaAte = agora.plus(DURACAO_BLOQUEIO);
        }
    }

    public void registrarSucesso() {
        tentativasFalhas = 0;
        bloqueadaAte = null;
    }

    public void liberarBloqueioExpirado(Instant agora) {
        if (bloqueadaAte != null && !agora.isBefore(bloqueadaAte)) {
            tentativasFalhas = 0;
            bloqueadaAte = null;
        }
    }
}
