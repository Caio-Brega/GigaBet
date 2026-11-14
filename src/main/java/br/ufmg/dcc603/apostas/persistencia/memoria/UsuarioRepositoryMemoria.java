package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class UsuarioRepositoryMemoria implements UsuarioRepository {
    private final EstadoMemoria estado;

    public UsuarioRepositoryMemoria(EstadoMemoria estado) {
        this.estado = estado;
    }

    @Override
    public void salvar(Usuario usuario) {
        estado.usuarios().put(usuario.id(), usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return Optional.ofNullable(estado.usuarios().get(id));
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        String normalizado = email.toLowerCase(Locale.ROOT);
        return estado.usuarios().values().stream()
                .filter(usuario -> usuario.email().equalsIgnoreCase(normalizado))
                .findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorCpf(String cpf) {
        return estado.usuarios().values().stream()
                .filter(Apostador.class::isInstance)
                .map(Apostador.class::cast)
                .filter(apostador -> apostador.cpf().equals(cpf))
                .map(apostador -> (Usuario) apostador)
                .findFirst();
    }

    @Override
    public List<Usuario> listar() {
        return new ArrayList<>(estado.usuarios().values());
    }
}
