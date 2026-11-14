package br.ufmg.dcc603.apostas.persistencia;

import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;

import java.util.List;

public interface AuditoriaRepository {
    void registrar(RegistroAuditoria registro);

    List<RegistroAuditoria> listar();
}
