package br.ufmg.dcc603.apostas.integracao.espn;

import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnEventDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnScoreboardDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnSummaryDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EspnBrasileiraoClientTest {

    private HttpServer server;
    private int port;
    private EspnBrasileiraoClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();

        client = new EspnBrasileiraoClient(
                "http://127.0.0.1:" + port,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build(),
                Duration.ofMillis(800));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requisitaScoreboardEDeserializaEventosComSucesso() {
        String jsonScoreboard = """
                {
                    "events": [
                        {
                            "id": "704838",
                            "date": "2026-11-20T23:00Z",
                            "status": {
                                "type": {
                                    "name": "STATUS_SCHEDULED",
                                    "description": "Scheduled"
                                }
                            },
                            "competitions": [
                                {
                                    "id": "704838",
                                    "competitors": [
                                        {
                                            "homeAway": "home",
                                            "team": {
                                                "id": "2026",
                                                "name": "Flamengo",
                                                "displayName": "Flamengo"
                                            }
                                        },
                                        {
                                            "homeAway": "away",
                                            "team": {
                                                "id": "2029",
                                                "name": "Palmeiras",
                                                "displayName": "Palmeiras"
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

        server.createContext("/scoreboard", exchange -> {
            byte[] bytes = jsonScoreboard.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        EspnScoreboardDto scoreboard = client.buscarScoreboard("20261120-20261125");
        assertNotNull(scoreboard);
        assertEquals(1, scoreboard.events().size());

        EspnEventDto event = scoreboard.events().get(0);
        assertEquals("704838", event.id());
        assertTrue(event.isScheduled());
        assertFalse(event.isFullTime());
        assertEquals("Flamengo", event.homeCompetitor().displayName());
        assertEquals("Palmeiras", event.awayCompetitor().displayName());
    }

    @Test
    void expandeIntervaloEmConsultasDiariasAceitasPelaEspn() {
        List<String> consultas = new ArrayList<>();
        server.createContext("/scoreboard", exchange -> {
            consultas.add(exchange.getRequestURI().getRawQuery());
            byte[] bytes = "{\"events\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        EspnScoreboardDto scoreboard = client.buscarScoreboard("20261118-20261120");

        assertTrue(scoreboard.events().isEmpty());
        assertEquals(List.of("dates=20261118", "dates=20261119", "dates=20261120"), consultas);
    }

    @Test
    void requisitaSummaryECalculaCartoesAmarelos() {
        String jsonSummary = """
                {
                    "id": "704838",
                    "details": [
                        {
                            "type": {"id": "1", "text": "Yellow Card"},
                            "athlete": {"displayName": "Gerson"},
                            "yellowCard": true
                        },
                        {
                            "type": {"id": "1", "text": "Cartão Amarelo"},
                            "athlete": {"displayName": "Veiga"}
                        },
                        {
                            "type": {"id": "2", "text": "Goal"},
                            "athlete": {"displayName": "Pedro"}
                        }
                    ]
                }
                """;

        server.createContext("/summary", exchange -> {
            byte[] bytes = jsonSummary.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        EspnSummaryDto summary = client.buscarResumo("704838");
        assertEquals("704838", summary.eventId());
        assertEquals(3, summary.details().size());
        assertEquals(2, summary.yellowCards());
    }

    @Test
    void lancaEspnApiExceptionEmCasoDeErroHttp() {
        server.createContext("/scoreboard", exchange -> {
            exchange.sendResponseHeaders(500, -1);
        });

        assertThrows(EspnApiException.class, () -> client.buscarScoreboard(null));
    }

    @Test
    void fallbackGraciosoRetornaObjetoVazioQuandoApiFalha() {
        server.createContext("/scoreboard", exchange -> {
            exchange.sendResponseHeaders(503, -1);
        });

        EspnScoreboardDto fallback = client.buscarScoreboardSeguro(null);
        assertNotNull(fallback);
        assertTrue(fallback.events().isEmpty());

        Optional<EspnSummaryDto> fallbackSummary = client.buscarResumoSeguro("9999");
        assertTrue(fallbackSummary.isEmpty());
    }

    @Test
    void lancaEspnApiExceptionEmCasoDeTimeout() {
        server.createContext("/scoreboard", exchange -> {
            try {
                Thread.sleep(1500); // Excede o timeout de 800ms
            } catch (InterruptedException ignored) {}
            exchange.sendResponseHeaders(200, -1);
        });

        assertThrows(EspnApiException.class, () -> client.buscarScoreboard(null));
    }
}
