package br.ufmg.dcc603.apostas.dominio;

public record ResultadoPartida(int golsMandante, int golsVisitante, int cartoesAmarelos) {
    public Desfecho desfecho() {
        if (golsMandante > golsVisitante) {
            return Desfecho.MANDANTE;
        }
        if (golsMandante < golsVisitante) {
            return Desfecho.VISITANTE;
        }
        return Desfecho.EMPATE;
    }

    public int cartoesAgrupados() {
        return Math.min(cartoesAmarelos, 10);
    }
}
