package br.ufmg.dcc603.apostas.dominio;

import java.math.BigDecimal;
import java.time.Instant;

public final class ApostaCartoesAmarelos extends Aposta {
    private final int total;

    public ApostaCartoesAmarelos(
            long id,
            long apostadorId,
            long partidaId,
            BigDecimal valor,
            BigDecimal probabilidade,
            BigDecimal premioPotencial,
            Instant criadaEm,
            int total) {
        super(id, apostadorId, partidaId, valor, probabilidade, premioPotencial, criadaEm);
        this.total = total;
    }

    private ApostaCartoesAmarelos(ApostaCartoesAmarelos origem) {
        super(origem);
        this.total = origem.total;
    }

    public int total() {
        return total;
    }

    @Override
    public TipoPrevisao tipo() {
        return TipoPrevisao.CARTOES_AMARELOS;
    }

    @Override
    public String descricaoPrevisao() {
        return total == 10 ? "10 ou mais cartoes amarelos" : total + " cartoes amarelos";
    }

    @Override
    public boolean avaliar(ResultadoPartida resultado) {
        return resultado.cartoesAgrupados() == total;
    }

    @Override
    public Aposta copiar() {
        return new ApostaCartoesAmarelos(this);
    }
}
