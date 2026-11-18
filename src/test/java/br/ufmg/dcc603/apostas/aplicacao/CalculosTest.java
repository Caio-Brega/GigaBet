package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.ApostaCartoesAmarelos;
import br.ufmg.dcc603.apostas.dominio.ApostaEquipeVencedora;
import br.ufmg.dcc603.apostas.dominio.ApostaPlacarExato;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.ResultadoPartida;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculosTest {
    private final CalculadoraProbabilidade probabilidade = CalculadoraProbabilidade.padrao();
    private final CalculadoraPremio premio = new CalculadoraPremio(ParametrosCalculo.PADRAO);
    private final Equipe casa = new Equipe(1, "Flamengo");
    private final Equipe fora = new Equipe(2, "Palmeiras");

    @Test
    void exemplosDaSprintDois() {
        List<Partida> historico = historicoDocumentado();
        Partida atual = new Partida(99, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));

        Estimativa vencedor = probabilidade.estimar(
                atual, PedidoAposta.vencedor(atual.id(), Desfecho.MANDANTE, new BigDecimal("50.00")), historico);
        assertEquals(TipoPrevisao.EQUIPE_VENCEDORA, vencedor.tipo());
        assertEquals(3, vencedor.espaco());
        assertEquals(new BigDecimal("92.72"), premio.calcular(new BigDecimal("50.00"), vencedor));

        Estimativa placar = probabilidade.estimar(
                atual, PedidoAposta.placar(atual.id(), 2, 1, new BigDecimal("10.00")), historico);
        assertEquals(TipoPrevisao.PLACAR_EXATO, placar.tipo());
        assertEquals(100, placar.espaco());
        // Odd realista em vez de odds distorcidas (R$ 103,23 em vez de R$ 486,00 para 10,00 apostados)
        assertEquals(new BigDecimal("103.23"), premio.calcular(new BigDecimal("10.00"), placar));
    }

    @Test
    void semHistoricoUsaPriorGlobal() {
        Partida atual = new Partida(1, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa vencedor = probabilidade.estimar(
                atual, PedidoAposta.vencedor(1, Desfecho.EMPATE, new BigDecimal("10.00")), List.of());
        assertEquals(TipoPrevisao.EQUIPE_VENCEDORA, vencedor.tipo());
        assertEquals(new BigDecimal("33.79"), premio.calcular(new BigDecimal("10.00"), vencedor));

        Estimativa cartoes = probabilidade.estimar(
                atual, PedidoAposta.cartoes(1, 4, new BigDecimal("11.00")), List.of());
        assertEquals(11, cartoes.espaco());
        assertEquals(TipoPrevisao.CARTOES_AMARELOS, cartoes.tipo());
        assertEquals(new BigDecimal("50.67"), premio.calcular(new BigDecimal("11.00"), cartoes));
    }

    @Test
    void oddsMinimaQuandoAProbabilidadeEAlta() {
        List<Partida> historico = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            historico.add(encerrada(i + 1, 5, 0, 1));
        }
        Partida atual = new Partida(100, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa estimativa = probabilidade.estimar(
                atual, PedidoAposta.vencedor(100, Desfecho.MANDANTE, new BigDecimal("100.00")), historico);
        // Com probabilidade muito alta, a odd calculada fica abaixo de 1.01 e e travada em 1.01
        assertEquals(new BigDecimal("101.00"), premio.calcular(new BigDecimal("100.00"), estimativa));
    }

    @Test
    void premioArredondaCentavos() {
        List<Partida> historico = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            historico.add(encerrada(i + 1, 1, 0, 0));
        }
        for (int i = 0; i < 2; i++) {
            historico.add(encerrada(i + 7, 0, 1, 0));
        }
        Partida atual = new Partida(20, casa, fora, Instant.parse("2026-12-01T19:00:00Z"));
        Estimativa estimativa = probabilidade.estimar(
                atual, PedidoAposta.vencedor(20, Desfecho.MANDANTE, new BigDecimal("10.00")), historico);
        assertEquals(new BigDecimal("20.19"), premio.calcular(new BigDecimal("10.00"), estimativa));
    }

    @Test
    void confrontoInvertidoEntraNoHistorico() {
        Partida invertida = new Partida(3, fora, casa, Instant.parse("2026-01-02T19:00:00Z"));
        invertida.encerrar(new ResultadoPartida(0, 2, 1));
        Partida atual = new Partida(4, casa, fora, Instant.parse("2026-02-01T19:00:00Z"));

        Estimativa estimativaInvertida = probabilidade.estimar(
                atual, PedidoAposta.vencedor(4, Desfecho.VISITANTE, new BigDecimal("10")), List.of(invertida));
        Estimativa estimativaSemHistorico = probabilidade.estimar(
                atual, PedidoAposta.vencedor(4, Desfecho.VISITANTE, new BigDecimal("10")), List.of());

        // O historico invertido influencia as forcas ofensiva e defensiva das equipes
        assertFalse(estimativaInvertida.probabilidade().equals(estimativaSemHistorico.probabilidade()));
    }

    @Test
    void avaliacaoDosTresTiposEAgrupamentoDeCartoes() {
        ResultadoPartida resultado = new ResultadoPartida(2, 1, 12);
        Instant agora = Instant.parse("2026-01-01T00:00:00Z");
        assertTrue(new ApostaEquipeVencedora(1, 1, 1, decimal("10"), decimal("0.3"), decimal("30"), agora, Desfecho.MANDANTE)
                .avaliar(resultado));
        assertFalse(new ApostaEquipeVencedora(2, 1, 1, decimal("10"), decimal("0.3"), decimal("30"), agora, Desfecho.EMPATE)
                .avaliar(resultado));
        assertTrue(new ApostaPlacarExato(3, 1, 1, decimal("10"), decimal("0.01"), decimal("100"), agora, 2, 1)
                .avaliar(resultado));
        assertFalse(new ApostaPlacarExato(4, 1, 1, decimal("10"), decimal("0.01"), decimal("100"), agora, 1, 2)
                .avaliar(resultado));
        assertTrue(new ApostaCartoesAmarelos(5, 1, 1, decimal("10"), decimal("0.1"), decimal("20"), agora, 10)
                .avaliar(resultado));
        assertFalse(new ApostaCartoesAmarelos(6, 1, 1, decimal("10"), decimal("0.1"), decimal("20"), agora, 9)
                .avaliar(resultado));
    }

    @Test
    void timeSemHistoricoUsaMediaDaLiga() {
        Equipe timeX = new Equipe(10, "Time X");
        Equipe timeY = new Equipe(11, "Time Y");
        Partida jogoLiga1 = new Partida(1, timeX, timeY, Instant.parse("2026-01-01T15:00:00Z"));
        jogoLiga1.encerrar(new ResultadoPartida(3, 1, 4));
        Partida jogoLiga2 = new Partida(2, timeX, timeY, Instant.parse("2026-01-02T15:00:00Z"));
        jogoLiga2.encerrar(new ResultadoPartida(3, 1, 6));

        List<Partida> historicoLiga = List.of(jogoLiga1, jogoLiga2);
        // Equipes casa (Flamengo) e fora (Palmeiras) nunca jogaram (cold start total)
        Partida partidaNova = new Partida(50, casa, fora, Instant.parse("2026-02-01T15:00:00Z"));

        EstatisticasPartida stats = EstatisticasPartida.calcular(partidaNova, historicoLiga);
        assertTrue(stats.isColdStartMandante());
        assertTrue(stats.isColdStartVisitante());
        // Media de gols mandante da liga = (3 + 3)/2 = 3.0
        assertEquals(3.0, stats.mediaGolsProMandante(), 0.001);
        // Media de gols visitante da liga = (1 + 1)/2 = 1.0
        assertEquals(1.0, stats.mediaGolsProVisitante(), 0.001);
        // Media de cartoes por partida da liga = (4 + 6)/2 = 5.0 (2.5 por equipe)
        assertEquals(5.0, stats.lambdaCartoes(), 0.001);
    }

    @Test
    void somaDasProbabilidadesIgualAUmEmCadaStrategy() {
        Partida atual = new Partida(70, casa, fora, Instant.parse("2026-03-01T19:00:00Z"));
        List<Partida> historico = historicoDocumentado();

        // 1. Placar Exato: soma de 0x0 a 9x9 deve ser 1.0
        BigDecimal somaPlacar = BigDecimal.ZERO;
        for (int x = 0; x <= 9; x++) {
            for (int y = 0; y <= 9; y++) {
                Estimativa e = probabilidade.estimar(atual, PedidoAposta.placar(atual.id(), x, y, BigDecimal.ONE), historico);
                somaPlacar = somaPlacar.add(e.probabilidade());
            }
        }
        assertEquals(1.0, somaPlacar.doubleValue(), 1e-9);
        assertEquals(new BigDecimal("1.000000"), somaPlacar.setScale(6, RoundingMode.HALF_UP));

        // 2. Cartoes Amarelos: soma de 0 a 10 deve ser 1.0
        BigDecimal somaCartoes = BigDecimal.ZERO;
        for (int k = 0; k <= 10; k++) {
            Estimativa e = probabilidade.estimar(atual, PedidoAposta.cartoes(atual.id(), k, BigDecimal.ONE), historico);
            somaCartoes = somaCartoes.add(e.probabilidade());
        }
        assertEquals(1.0, somaCartoes.doubleValue(), 1e-9);
        assertEquals(new BigDecimal("1.000000"), somaCartoes.setScale(6, RoundingMode.HALF_UP));

        // 3. Equipe Vencedora: soma de MANDANTE, EMPATE, VISITANTE deve ser 1.0
        BigDecimal somaVencedor = BigDecimal.ZERO;
        for (Desfecho d : Desfecho.values()) {
            Estimativa e = probabilidade.estimar(atual, PedidoAposta.vencedor(atual.id(), d, BigDecimal.ONE), historico);
            somaVencedor = somaVencedor.add(e.probabilidade());
        }
        assertEquals(1.0, somaVencedor.doubleValue(), 1e-9);
        assertEquals(new BigDecimal("1.000000"), somaVencedor.setScale(6, RoundingMode.HALF_UP));
    }

    @Test
    void aplicacaoCorretaDoTetoMaximoDeOddETetoFinanceiro() {
        // Probabilidade minuscula (evento raro)
        BigDecimal probMinima = new BigDecimal("0.000001");

        // 1. Teto por mercado: Placar Exato max 25.0
        BigDecimal oddPlacar = premio.calcularOdd(probMinima, TipoPrevisao.PLACAR_EXATO);
        assertEquals(new BigDecimal("25.00"), oddPlacar.setScale(2, RoundingMode.HALF_UP));

        // 2. Teto por mercado: Cartoes max 15.0
        BigDecimal oddCartoes = premio.calcularOdd(probMinima, TipoPrevisao.CARTOES_AMARELOS);
        assertEquals(new BigDecimal("15.00"), oddCartoes.setScale(2, RoundingMode.HALF_UP));

        // 3. Teto por mercado: Equipe Vencedora max 10.0
        BigDecimal oddVencedor = premio.calcularOdd(probMinima, TipoPrevisao.EQUIPE_VENCEDORA);
        assertEquals(new BigDecimal("10.00"), oddVencedor.setScale(2, RoundingMode.HALF_UP));

        // 4. Teto financeiro absoluto por bilhete (MAX_PAYOUT)
        // Aposta de 10.000 com odd de 25.0 daria 250.000, mas deve ser limitada a 100.000 (MAX_PAYOUT)
        Estimativa estimativaRara = new Estimativa(probMinima, TipoPrevisao.PLACAR_EXATO);
        BigDecimal premioCalculado = premio.calcular(new BigDecimal("10000.00"), estimativaRara);
        assertEquals(new BigDecimal("100000.00"), premioCalculado);
    }

    @Test
    void cenarioDePlacarComumRetornaOddsRealistas() {
        Partida atual = new Partida(80, casa, fora, Instant.parse("2026-04-01T19:00:00Z"));
        List<Partida> historico = historicoDocumentado();

        Estimativa placar1x0 = probabilidade.estimar(atual, PedidoAposta.placar(atual.id(), 1, 0, BigDecimal.TEN), historico);
        BigDecimal odd1x0 = premio.calcularOdd(placar1x0.probabilidade(), TipoPrevisao.PLACAR_EXATO);

        Estimativa placar2x1 = probabilidade.estimar(atual, PedidoAposta.placar(atual.id(), 2, 1, BigDecimal.TEN), historico);
        BigDecimal odd2x1 = premio.calcularOdd(placar2x1.probabilidade(), TipoPrevisao.PLACAR_EXATO);

        // Odds de placares comuns como 1x0 e 2x1 devem estar entre 5.0 e 12.0
        assertTrue(odd1x0.compareTo(new BigDecimal("5.0")) >= 0 && odd1x0.compareTo(new BigDecimal("12.0")) <= 0,
                "Odd de 1x0 deve estar entre 5.0 e 12.0, mas foi " + odd1x0);
        assertTrue(odd2x1.compareTo(new BigDecimal("5.0")) >= 0 && odd2x1.compareTo(new BigDecimal("12.0")) <= 0,
                "Odd de 2x1 deve estar entre 5.0 e 12.0, mas foi " + odd2x1);
    }

    private List<Partida> historicoDocumentado() {
        int[][] placares = {{2, 1}, {1, 0}, {3, 0}, {2, 0}, {0, 1}, {0, 2}, {1, 1}, {2, 2}};
        List<Partida> partidas = new ArrayList<>();
        for (int i = 0; i < placares.length; i++) {
            partidas.add(encerrada(i + 1, placares[i][0], placares[i][1], 2));
        }
        return partidas;
    }

    private Partida encerrada(long id, int golsMandante, int golsVisitante, int cartoes) {
        Partida partida = new Partida(id, casa, fora, Instant.parse("2026-01-01T19:00:00Z").plusSeconds(id));
        partida.encerrar(new ResultadoPartida(golsMandante, golsVisitante, cartoes));
        return partida;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
