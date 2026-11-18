package br.ufmg.dcc603.apostas.aplicacao;

public final class DistribuicaoPoisson {
    private DistribuicaoPoisson() {}

    /**
     * Calcula a probabilidade pontual P(X = k) sob Poisson(lambda):
     * P(X = k) = (e^(-lambda) * lambda^k) / k!
     */
    public static double probabilidade(double lambda, int k) {
        if (k < 0) {
            return 0.0;
        }
        if (lambda <= 0.0) {
            return k == 0 ? 1.0 : 0.0;
        }
        double p = Math.exp(-lambda);
        for (int i = 1; i <= k; i++) {
            p = (p * lambda) / i;
        }
        return p;
    }

    /**
     * Calcula a probabilidade acumulada P(X <= ateK).
     */
    public static double probabilidadeAcumulada(double lambda, int ateK) {
        if (ateK < 0) {
            return 0.0;
        }
        double soma = 0.0;
        for (int i = 0; i <= ateK; i++) {
            soma += probabilidade(lambda, i);
        }
        return Math.min(1.0, soma);
    }
}
