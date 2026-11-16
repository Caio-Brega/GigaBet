package br.ufmg.dcc603.apostas.persistencia.jdbc;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.ApostaCartoesAmarelos;
import br.ufmg.dcc603.apostas.dominio.ApostaEquipeVencedora;
import br.ufmg.dcc603.apostas.dominio.ApostaPlacarExato;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class ApostaRepositoryJdbc implements ApostaRepository {
    private static final String COLUNAS = """
            id, apostador_id, partida_id, tipo, valor, probabilidade, premio_potencial,
            criada_em, status, desfecho, gols_mandante, gols_visitante, cartoes
            """;

    private final Banco banco;

    public ApostaRepositoryJdbc(Banco banco) {
        this.banco = banco;
    }

    @Override
    public void salvar(Aposta aposta) {
        String desfecho = null;
        Integer golsMandante = null;
        Integer golsVisitante = null;
        Integer cartoes = null;
        if (aposta instanceof ApostaEquipeVencedora vencedor) {
            desfecho = vencedor.desfecho().name();
        } else if (aposta instanceof ApostaPlacarExato placar) {
            golsMandante = placar.golsMandante();
            golsVisitante = placar.golsVisitante();
        } else if (aposta instanceof ApostaCartoesAmarelos amarelos) {
            cartoes = amarelos.total();
        }
        banco.atualizar("""
                INSERT INTO apostas (
                    id, apostador_id, partida_id, tipo, valor, probabilidade, premio_potencial,
                    criada_em, status, desfecho, gols_mandante, gols_visitante, cartoes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    apostador_id = EXCLUDED.apostador_id,
                    partida_id = EXCLUDED.partida_id,
                    tipo = EXCLUDED.tipo,
                    valor = EXCLUDED.valor,
                    probabilidade = EXCLUDED.probabilidade,
                    premio_potencial = EXCLUDED.premio_potencial,
                    criada_em = EXCLUDED.criada_em,
                    status = EXCLUDED.status,
                    desfecho = EXCLUDED.desfecho,
                    gols_mandante = EXCLUDED.gols_mandante,
                    gols_visitante = EXCLUDED.gols_visitante,
                    cartoes = EXCLUDED.cartoes
                """,
                aposta.id(),
                aposta.apostadorId(),
                aposta.partidaId(),
                aposta.tipo(),
                aposta.valor(),
                aposta.probabilidade(),
                aposta.premioPotencial(),
                aposta.criadaEm(),
                aposta.status(),
                desfecho,
                golsMandante,
                golsVisitante,
                cartoes);
    }

    @Override
    public List<Aposta> listar() {
        return banco.listar("SELECT " + COLUNAS + " FROM apostas ORDER BY id", this::mapear);
    }

    @Override
    public List<Aposta> listarPorApostador(long apostadorId) {
        return banco.listar(
                "SELECT " + COLUNAS + " FROM apostas WHERE apostador_id = ? ORDER BY id",
                this::mapear, apostadorId);
    }

    @Override
    public List<Aposta> listarPorPartida(long partidaId) {
        return banco.listar(
                "SELECT " + COLUNAS + " FROM apostas WHERE partida_id = ? ORDER BY id",
                this::mapear, partidaId);
    }

    @Override
    public int contarAbertas(long apostadorId) {
        return banco.consultarUm("""
                SELECT COUNT(*) AS total FROM apostas
                WHERE apostador_id = ? AND status = 'ABERTA'
                """, linhas -> linhas.getInt("total"), apostadorId).orElse(0);
    }

    @Override
    public boolean existeAberta(long apostadorId, long partidaId) {
        return banco.consultarUm("""
                SELECT 1 AS achou FROM apostas
                WHERE apostador_id = ? AND partida_id = ? AND status = 'ABERTA'
                """, linhas -> linhas.getInt("achou"), apostadorId, partidaId).isPresent();
    }

    private Aposta mapear(ResultSet linhas) throws SQLException {
        TipoPrevisao tipo = TipoPrevisao.valueOf(linhas.getString("tipo"));
        Aposta aposta = switch (tipo) {
            case EQUIPE_VENCEDORA -> new ApostaEquipeVencedora(
                    linhas.getLong("id"),
                    linhas.getLong("apostador_id"),
                    linhas.getLong("partida_id"),
                    linhas.getBigDecimal("valor"),
                    linhas.getBigDecimal("probabilidade"),
                    linhas.getBigDecimal("premio_potencial"),
                    linhas.getTimestamp("criada_em").toInstant(),
                    Desfecho.valueOf(linhas.getString("desfecho")));
            case PLACAR_EXATO -> new ApostaPlacarExato(
                    linhas.getLong("id"),
                    linhas.getLong("apostador_id"),
                    linhas.getLong("partida_id"),
                    linhas.getBigDecimal("valor"),
                    linhas.getBigDecimal("probabilidade"),
                    linhas.getBigDecimal("premio_potencial"),
                    linhas.getTimestamp("criada_em").toInstant(),
                    linhas.getInt("gols_mandante"),
                    linhas.getInt("gols_visitante"));
            case CARTOES_AMARELOS -> new ApostaCartoesAmarelos(
                    linhas.getLong("id"),
                    linhas.getLong("apostador_id"),
                    linhas.getLong("partida_id"),
                    linhas.getBigDecimal("valor"),
                    linhas.getBigDecimal("probabilidade"),
                    linhas.getBigDecimal("premio_potencial"),
                    linhas.getTimestamp("criada_em").toInstant(),
                    linhas.getInt("cartoes"));
        };
        aposta.restaurarStatus(StatusAposta.valueOf(linhas.getString("status")));
        return aposta;
    }
}
