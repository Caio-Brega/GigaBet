package br.ufmg.dcc603.apostas.dominio;

import java.math.BigDecimal;
import java.time.Instant;

public final class ApostaPlacarExato extends Aposta {
    private final int golsMandante;
    private final int golsVisitante;

    public ApostaPlacarExato(
            long id,
            long apostadorId,
            long partidaId,
            BigDecimal valor,
            BigDecimal probabilidade,
            BigDecimal premioPotencial,
            Instant criadaEm,
            int golsMandante,
            int golsVisitante) {
        super(id, apostadorId, partidaId, valor, probabilidade, premioPotencial, criadaEm);
        this.golsMandante = golsMandante;
        this.golsVisitante = golsVisitante;
    }

    private ApostaPlacarExato(ApostaPlacarExato origem) {
        super(origem);
        this.golsMandante = origem.golsMandante;
        this.golsVisitante = origem.golsVisitante;
    }

    public int golsMandante() {
        return golsMandante;
    }

    public int golsVisitante() {
        return golsVisitante;
    }

    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.PLACAR_EXATO;
    }

    @Override
    public String descricaoPrevisao() {
        return golsMandante + " x " + golsVisitante;
    }

    @Override
    public boolean avaliar(ResultadoPartida resultado) {
        return resultado.golsMandante() == golsMandante && resultado.golsVisitante() == golsVisitante;
    }

    @Override
    public Aposta copiar() {
        return new ApostaPlacarExato(this);
    }
}
