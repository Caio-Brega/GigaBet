package br.ufmg.dcc603.apostas.integracao.espn;

public class EspnApiException extends RuntimeException {
    public EspnApiException(String message) {
        super(message);
    }

    public EspnApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
