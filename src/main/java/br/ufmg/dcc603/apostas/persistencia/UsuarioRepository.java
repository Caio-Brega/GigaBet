package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.dominio.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    void salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(long id);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorCpf(String cpf);

    List<Usuario> listar();
}
