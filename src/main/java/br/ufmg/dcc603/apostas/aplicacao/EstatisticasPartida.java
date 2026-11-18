package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;

import java.util.Comparator;
import java.util.List;

public final class EstatisticasPartida {
    public static final int LIMITE_JOGOS_RECENTES = 10;
    public static final double PRIOR_GOLS_MANDANTE = 1.4;
    public static final double PRIOR_GOLS_VISITANTE = 1.1;
    public static final double PRIOR_CARTOES_PARTIDA = 4.0;
    public static final double LAMBDA_MINIMO = 0.1;

    private final double lambdaMandante;
    private final double lambdaVisitante;
    private final double lambdaCartoes;
    private final double mediaGolsProMandante;
    private final double mediaGolsContraMandante;
    private final double mediaCartoesMandante;
    private final double mediaGolsProVisitante;
    private final double mediaGolsContraVisitante;
    private final double mediaCartoesVisitante;
    private final boolean coldStartMandante;
    private final boolean coldStartVisitante;
    private final int historicoGeralTotal;

    private EstatisticasPartida(
            double lambdaMandante,
            double lambdaVisitante,
            double lambdaCartoes,
            double mediaGolsProMandante,
            double mediaGolsContraMandante,
            double mediaCartoesMandante,
            double mediaGolsProVisitante,
            double mediaGolsContraVisitante,
            double mediaCartoesVisitante,
            boolean coldStartMandante,
            boolean coldStartVisitante,
            int historicoGeralTotal) {
        this.lambdaMandante = lambdaMandante;
        this.lambdaVisitante = lambdaVisitante;
        this.lambdaCartoes = lambdaCartoes;
        this.mediaGolsProMandante = mediaGolsProMandante;
        this.mediaGolsContraMandante = mediaGolsContraMandante;
        this.mediaCartoesMandante = mediaCartoesMandante;
        this.mediaGolsProVisitante = mediaGolsProVisitante;
        this.mediaGolsContraVisitante = mediaGolsContraVisitante;
        this.mediaCartoesVisitante = mediaCartoesVisitante;
        this.coldStartMandante = coldStartMandante;
        this.coldStartVisitante = coldStartVisitante;
        this.historicoGeralTotal = historicoGeralTotal;
    }

    public static EstatisticasPartida calcular(Partida atual, List<Partida> todasPartidas) {
        List<Partida> encerradas = todasPartidas.stream()
                .filter(p -> p.status() == StatusPartida.ENCERRADA)
                .filter(p -> p.resultado() != null)
                .filter(p -> p.id() != atual.id())
                .toList();

        double mediaLigaGolsMandante;
        double mediaLigaGolsVisitante;
        double mediaLigaCartoesEquipe;

        if (encerradas.isEmpty()) {
            mediaLigaGolsMandante = PRIOR_GOLS_MANDANTE;
            mediaLigaGolsVisitante = PRIOR_GOLS_VISITANTE;
            mediaLigaCartoesEquipe = PRIOR_CARTOES_PARTIDA / 2.0;
        } else {
            double somaGolsM = 0;
            double somaGolsV = 0;
            double somaCartoes = 0;
            for (Partida p : encerradas) {
                ResultadoPartida r = p.resultado();
                somaGolsM += r.golsMandante();
                somaGolsV += r.golsVisitante();
                somaCartoes += r.cartoesAmarelos();
            }
            mediaLigaGolsMandante = somaGolsM / encerradas.size();
            mediaLigaGolsVisitante = somaGolsV / encerradas.size();
            mediaLigaCartoesEquipe = (somaCartoes / encerradas.size()) / 2.0;
        }

        Equipe mandante = atual.mandante();
        Equipe visitante = atual.visitante();

        List<Partida> jogosMandante = extrairJogosRecentes(encerradas, mandante.id());
        List<Partida> jogosVisitante = extrairJogosRecentes(encerradas, visitante.id());

        boolean coldStartMandante = jogosMandante.isEmpty();
        boolean coldStartVisitante = jogosVisitante.isEmpty();

        double proM;
        double contraM;
        double cartoesM;
        if (coldStartMandante) {
            proM = mediaLigaGolsMandante;
            contraM = mediaLigaGolsVisitante;
            cartoesM = mediaLigaCartoesEquipe;
        } else {
            MediasEquipe medias = calcularMedias(jogosMandante, mandante.id());
            proM = medias.golsPro;
            contraM = medias.golsContra;
            cartoesM = medias.cartoesEquipe;
        }

        double proV;
        double contraV;
        double cartoesV;
        if (coldStartVisitante) {
            proV = mediaLigaGolsVisitante;
            contraV = mediaLigaGolsMandante;
            cartoesV = mediaLigaCartoesEquipe;
        } else {
            MediasEquipe medias = calcularMedias(jogosVisitante, visitante.id());
            proV = medias.golsPro;
            contraV = medias.golsContra;
            cartoesV = medias.cartoesEquipe;
        }

        double lMandante = Math.max(LAMBDA_MINIMO, (proM + contraV) / 2.0);
        double lVisitante = Math.max(LAMBDA_MINIMO, (proV + contraM) / 2.0);
        double lCartoes = Math.max(LAMBDA_MINIMO, cartoesM + cartoesV);

        return new EstatisticasPartida(
                lMandante,
                lVisitante,
                lCartoes,
                proM,
                contraM,
                cartoesM,
                proV,
                contraV,
                cartoesV,
                coldStartMandante,
                coldStartVisitante,
                encerradas.size());
    }

    private static List<Partida> extrairJogosRecentes(List<Partida> encerradas, long equipeId) {
        return encerradas.stream()
                .filter(p -> p.mandante().id() == equipeId || p.visitante().id() == equipeId)
                .sorted(Comparator.comparing(Partida::inicio).reversed())
                .limit(LIMITE_JOGOS_RECENTES)
                .toList();
    }

    private static MediasEquipe calcularMedias(List<Partida> jogos, long equipeId) {
        int count = jogos.size();
        double somaPro = 0;
        double somaContra = 0;
        double somaCartoes = 0;
        for (Partida p : jogos) {
            ResultadoPartida r = p.resultado();
            if (p.mandante().id() == equipeId) {
                somaPro += r.golsMandante();
                somaContra += r.golsVisitante();
            } else {
                somaPro += r.golsVisitante();
                somaContra += r.golsMandante();
            }
            somaCartoes += r.cartoesAmarelos();
        }
        return new MediasEquipe(somaPro / count, somaContra / count, (somaCartoes / count) / 2.0);
    }

    private record MediasEquipe(double golsPro, double golsContra, double cartoesEquipe) {}

    public double lambdaMandante() {
        return lambdaMandante;
    }

    public double lambdaVisitante() {
        return lambdaVisitante;
    }

    public double lambdaCartoes() {
        return lambdaCartoes;
    }

    public double mediaGolsProMandante() {
        return mediaGolsProMandante;
    }

    public double mediaGolsContraMandante() {
        return mediaGolsContraMandante;
    }

    public double mediaCartoesMandante() {
        return mediaCartoesMandante;
    }

    public double mediaGolsProVisitante() {
        return mediaGolsProVisitante;
    }

    public double mediaGolsContraVisitante() {
        return mediaGolsContraVisitante;
    }

    public double mediaCartoesVisitante() {
        return mediaCartoesVisitante;
    }

    public boolean isColdStartMandante() {
        return coldStartMandante;
    }

    public boolean isColdStartVisitante() {
        return coldStartVisitante;
    }

    public int historicoGeralTotal() {
        return historicoGeralTotal;
    }
}
