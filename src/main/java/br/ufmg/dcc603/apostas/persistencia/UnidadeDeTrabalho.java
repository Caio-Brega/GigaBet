package br.ufmg.dcc603.apostas.persistencia;

import java.util.function.Supplier;

public interface UnidadeDeTrabalho {
    <T> T executar(Supplier<T> acao);

    long proximoId();
}
