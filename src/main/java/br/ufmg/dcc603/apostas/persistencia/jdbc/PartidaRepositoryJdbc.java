package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class PartidaRepositoryJdbc implements PartidaRepository {
    private final Banco banco;
    private final EquipeRepository equipes;

    public PartidaRepositoryJdbc(Banco banco, EquipeRepository equipes) {
        this.banco = banco;
        this.equipes = equipes;
    }

    @Override
    public void salvar(Partida partida) {
        Integer golsMandante = null;
        Integer golsVisitante = null;
        Integer cartoes = null;
        if (partida.resultado() != null) {
            golsMandante = partida.resultado().golsMandante();
            golsVisitante = partida.resultado().golsVisitante();
            cartoes = partida.resultado().cartoesAmarelos();
        }
        banco.atualizar("""
                INSERT INTO partidas (
                    id, mandante_id, visitante_id, inicio, status,
                    gols_mandante, gols_visitante, cartoes_amarelos
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    mandante_id = EXCLUDED.mandante_id,
                    visitante_id = EXCLUDED.visitante_id,
                    inicio = EXCLUDED.inicio,
                    status = EXCLUDED.status,
                    gols_mandante = EXCLUDED.gols_mandante,
                    gols_visitante = EXCLUDED.gols_visitante,
                    cartoes_amarelos = EXCLUDED.cartoes_amarelos
                """,
                partida.id(),
                partida.mandante().id(),
                partida.visitante().id(),
                partida.inicio(),
                partida.status(),
                golsMandante,
                golsVisitante,
                cartoes);
    }

    @Override
    public Optional<Partida> buscarPorId(long id) {
        return banco.consultarUm("""
                SELECT id, mandante_id, visitante_id, inicio, status,
                       gols_mandante, gols_visitante, cartoes_amarelos
                FROM partidas WHERE id = ?
                """, this::ler, id).map(this::montar);
    }

    @Override
    public List<Partida> listar() {
        return banco.listar("""
                SELECT id, mandante_id, visitante_id, inicio, status,
                       gols_mandante, gols_visitante, cartoes_amarelos
                FROM partidas ORDER BY inicio, id
                """, this::ler).stream().map(this::montar).toList();
    }

    private Linha ler(ResultSet linhas) throws SQLException {
        Instant inicio = linhas.getTimestamp("inicio").toInstant();
        StatusPartida status = StatusPartida.valueOf(linhas.getString("status"));
        int golsMandante = linhas.getInt("gols_mandante");
        boolean semGols = linhas.wasNull();
        ResultadoPartida resultado = null;
        if (!semGols) {
            resultado = new ResultadoPartida(
                    golsMandante, linhas.getInt("gols_visitante"), linhas.getInt("cartoes_amarelos"));
        }
        return new Linha(
                linhas.getLong("id"),
                linhas.getLong("mandante_id"),
                linhas.getLong("visitante_id"),
                inicio,
                status,
                resultado);
    }

    private Partida montar(Linha linha) {
        Equipe mandante = equipes.buscarPorId(linha.mandanteId())
                .orElseThrow(() -> new IllegalStateException("Mandante da partida nao encontrado"));
        Equipe visitante = equipes.buscarPorId(linha.visitanteId())
                .orElseThrow(() -> new IllegalStateException("Visitante da partida nao encontrado"));
        Partida partida = new Partida(linha.id(), mandante, visitante, linha.inicio());
        if (linha.status() != StatusPartida.AGENDADA || linha.resultado() != null) {
            partida.restaurar(linha.status(), linha.resultado());
        }
        return partida;
    }

    private record Linha(
            long id,
            long mandanteId,
            long visitanteId,
            Instant inicio,
            StatusPartida status,
            ResultadoPartida resultado) {
    }
}
