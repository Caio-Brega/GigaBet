package br.ufmg.dcc603.apostas.dominio;

public final class Administrador extends Usuario {
    public Administrador(long id, String nome, String email, String senhaHash) {
        super(id, nome, email, senhaHash);
    }

    private Administrador(Administrador origem) {
        super(origem);
    }

    @Override
    public Perfil perfil() {
        return Perfil.ADMINISTRADOR;
    }

    @Override
    public Administrador copiar() {
        return new Administrador(this);
    }
}
