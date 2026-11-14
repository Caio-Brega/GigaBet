package br.ufmg.dcc603.apostas.aplicacao;

public final class RegraNegocioException extends RuntimeException {
    private final CodigoRegra codigo;

    public RegraNegocioException(CodigoRegra codigo) {
        super(codigo.mensagem());
        this.codigo = codigo;
    }

    public RegraNegocioException(CodigoRegra codigo, String detalhe) {
        super(detalhe);
        this.codigo = codigo;
    }

    public CodigoRegra codigo() {
        return codigo;
    }
}
