package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;
import br.ufmg.dcc603.apostas.persistencia.AuditoriaRepository;

import java.util.List;

public final class AuditoriaRepositoryMemoria implements AuditoriaRepository {
    private final EstadoMemoria estado;

    public AuditoriaRepositoryMemoria(EstadoMemoria estado) {
        this.estado = estado;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        estado.auditoria().add(registro);
    }

    @Override
    public List<RegistroAuditoria> listar() {
        return List.copyOf(estado.auditoria());
    }
}
