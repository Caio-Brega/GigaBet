package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.persistencia.AuditoriaRepository;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;

import java.time.Clock;
import java.util.List;

public final class AuditoriaService {
    private final AuditoriaRepository auditoria;
    private final UnidadeDeTrabalho unidade;
    private final Clock relogio;

    public AuditoriaService(AuditoriaRepository auditoria, UnidadeDeTrabalho unidade, Clock relogio) {
        this.auditoria = auditoria;
        this.unidade = unidade;
        this.relogio = relogio;
    }

    public void registrar(TipoEventoAuditoria tipo, Long usuarioId, String descricao) {
        auditoria.registrar(new RegistroAuditoria(
                unidade.proximoId(), relogio.instant(), tipo, usuarioId, descricao));
    }

    public List<RegistroAuditoria> listar() {
        return auditoria.listar();
    }
}
