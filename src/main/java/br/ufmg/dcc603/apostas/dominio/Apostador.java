package br.ufmg.dcc603.apostas.dominio;

import java.math.BigDecimal;

public final class Apostador extends Usuario {
    private final String cpf;
    private final boolean consentimento;
    private BigDecimal saldo;

    public Apostador(long id, String nome, String email, String senhaHash, String cpf, boolean consentimento) {
        super(id, nome, email, senhaHash);
        this.cpf = cpf;
        this.consentimento = consentimento;
        this.saldo = new BigDecimal("0.00");
    }

    private Apostador(Apostador origem) {
        super(origem);
        this.cpf = origem.cpf;
        this.consentimento = origem.consentimento;
        this.saldo = origem.saldo;
    }

    public String cpf() {
        return cpf;
    }

    public boolean consentimento() {
        return consentimento;
    }

    public BigDecimal saldo() {
        return saldo;
    }

    public void creditar(BigDecimal valor) {
        saldo = saldo.add(valor);
    }

    public void debitar(BigDecimal valor) {
        if (saldo.compareTo(valor) < 0) {
            throw new IllegalStateException("Saldo insuficiente para debito");
        }
        saldo = saldo.subtract(valor);
    }

    @Override
    public Perfil perfil() {
        return Perfil.APOSTADOR;
    }

    @Override
    public Apostador copiar() {
        return new Apostador(this);
    }
}
