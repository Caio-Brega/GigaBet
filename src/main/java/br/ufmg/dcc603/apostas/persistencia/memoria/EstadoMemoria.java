package br.ufmg.dcc603.apostas.persistencia.memoria;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class EstadoMemoria implements UnidadeDeTrabalho {
    private Map<Long, Usuario> usuarios = new HashMap<>();
    private Map<Long, Equipe> equipes = new HashMap<>();
    private Map<Long, Partida> partidas = new HashMap<>();
    private Map<Long, Aposta> apostas = new HashMap<>();
    private List<RegistroAuditoria> auditoria = new ArrayList<>();
    private long sequencia = 1;

    public Map<Long, Usuario> usuarios() {
        return usuarios;
    }

    public Map<Long, Equipe> equipes() {
        return equipes;
    }

    public Map<Long, Partida> partidas() {
        return partidas;
    }

    public Map<Long, Aposta> apostas() {
        return apostas;
    }

    public List<RegistroAuditoria> auditoria() {
        return auditoria;
    }

    @Override
    public long proximoId() {
        return sequencia++;
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        Instantaneo anterior = fotografar();
        try {
            return acao.get();
        } catch (RuntimeException e) {
            restaurar(anterior);
            throw e;
        }
    }

    private Instantaneo fotografar() {
        Map<Long, Usuario> copiaUsuarios = new HashMap<>();
        usuarios.forEach((id, usuario) -> copiaUsuarios.put(id, usuario.copiar()));
        Map<Long, Equipe> copiaEquipes = new HashMap<>(equipes);
        Map<Long, Partida> copiaPartidas = new HashMap<>();
        partidas.forEach((id, partida) -> copiaPartidas.put(id, partida.copiar()));
        Map<Long, Aposta> copiaApostas = new HashMap<>();
        apostas.forEach((id, aposta) -> copiaApostas.put(id, aposta.copiar()));
        return new Instantaneo(copiaUsuarios, copiaEquipes, copiaPartidas, copiaApostas,
                new ArrayList<>(auditoria), sequencia);
    }

    private void restaurar(Instantaneo anterior) {
        usuarios = anterior.usuarios;
        equipes = anterior.equipes;
        partidas = anterior.partidas;
        apostas = anterior.apostas;
        auditoria = anterior.auditoria;
        sequencia = anterior.sequencia;
    }

    private record Instantaneo(
            Map<Long, Usuario> usuarios,
            Map<Long, Equipe> equipes,
            Map<Long, Partida> partidas,
            Map<Long, Aposta> apostas,
            List<RegistroAuditoria> auditoria,
            long sequencia) {
    }
}
