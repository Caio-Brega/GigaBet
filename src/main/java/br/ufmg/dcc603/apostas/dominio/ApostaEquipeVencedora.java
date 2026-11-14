package br.ufmg.dcc603.apostas.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class ApostaEquipeVencedora extends Aposta {
    private final Desfecho desfecho;

    public ApostaEquipeVencedora(
            long id,
            long apostadorId,
            long partidaId,
            BigDecimal valor,
            BigDecimal probabilidade,
            BigDecimal premioPotencial,
            Instant criadaEm,
            Desfecho desfecho) {
        super(id, apostadorId, partidaId, valor, probabilidade, premioPotencial, criadaEm);
        this.desfecho = Objects.requireNonNull(desfecho);
    }

    private ApostaEquipeVencedora(ApostaEquipeVencedora origem) {
        super(origem);
        this.desfecho = origem.desfecho;
    }

    public Desfecho desfecho() {
        return desfecho;
    }

    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.EQUIPE_VENCEDORA;
    }

    @Override
    public String descricaoPrevisao() {
        return switch (desfecho) {
            case MANDANTE -> "vitoria do mandante";
            case EMPATE -> "empate";
            case VISITANTE -> "vitoria do visitante";
        };
    }

    @Override
    public boolean avaliar(ResultadoPartida resultado) {
        return resultado.desfecho() == desfecho;
    }

    @Override
    public Aposta copiar() {
        return new ApostaEquipeVencedora(this);
    }
}
