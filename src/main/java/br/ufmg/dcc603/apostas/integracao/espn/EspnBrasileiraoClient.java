package br.ufmg.dcc603.apostas.integracao.espn;

import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnScoreboardDto;
import br.ufmg.dcc603.apostas.integracao.espn.dto.EspnSummaryDto;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EspnBrasileiraoClient {
    public static final String DEFAULT_BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer/bra.1";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Duration timeout;

    public EspnBrasileiraoClient() {
        this(DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), DEFAULT_TIMEOUT);
    }

    public EspnBrasileiraoClient(String baseUrl, HttpClient httpClient) {
        this(baseUrl, httpClient, DEFAULT_TIMEOUT);
    }

    public EspnBrasileiraoClient(String baseUrl, HttpClient httpClient, Duration timeout) {
        String url = baseUrl != null ? baseUrl.trim() : DEFAULT_BASE_URL;
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.httpClient = httpClient;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public Duration timeout() {
        return timeout;
    }

    public EspnScoreboardDto buscarScoreboard(String dates) {
        if (dates != null && dates.matches("\\d{8}-\\d{8}")) {
            return buscarScoreboardIntervalo(dates);
        }
        return buscarScoreboardData(dates);
    }

    private EspnScoreboardDto buscarScoreboardIntervalo(String dates) {
        String[] limites = dates.split("-", 2);
        DateTimeFormatter formato = DateTimeFormatter.BASIC_ISO_DATE;
        LocalDate inicio;
        LocalDate fim;
        try {
            inicio = LocalDate.parse(limites[0], formato);
            fim = LocalDate.parse(limites[1], formato);
        } catch (DateTimeParseException e) {
            throw new EspnApiException("Intervalo de datas invalido para consultar ESPN: " + dates, e);
        }
        if (fim.isBefore(inicio)) {
            throw new EspnApiException("Data final anterior a data inicial no intervalo ESPN: " + dates);
        }

        Map<String, br.ufmg.dcc603.apostas.integracao.espn.dto.EspnEventDto> eventos = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            EspnScoreboardDto doDia = buscarScoreboardData(dia.format(formato));
            for (var evento : doDia.events()) {
                String chave = evento.id() == null || evento.id().isBlank()
                        ? dia + "-" + eventos.size()
                        : evento.id();
                eventos.putIfAbsent(chave, evento);
            }
        }
        return new EspnScoreboardDto(List.copyOf(eventos.values()));
    }

    private EspnScoreboardDto buscarScoreboardData(String dates) {
        String url = baseUrl + "/scoreboard";
        if (dates != null && !dates.isBlank()) {
            url += "?dates=" + dates.trim();
        }
        String json = executarRequisicao(url);
        if (json == null || json.isBlank()) {
            return new EspnScoreboardDto(java.util.List.of());
        }
        try {
            JsonObject root = JsonParser.parseObject(json);
            return EspnScoreboardDto.fromJson(root);
        } catch (RuntimeException e) {
            throw new EspnApiException("Resposta JSON invalida da ESPN para scoreboard", e);
        }
    }

    public EspnScoreboardDto buscarScoreboardSeguro(String dates) {
        try {
            return buscarScoreboard(dates);
        } catch (EspnApiException e) {
            System.err.println("[ESPN Client] Fallback ativado ao buscar scoreboard: " + e.getMessage());
            return new EspnScoreboardDto(java.util.List.of());
        }
    }

    public EspnSummaryDto buscarResumo(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId obrigatorio");
        }
        String url = baseUrl + "/summary?event=" + eventId.trim();
        String json = executarRequisicao(url);
        if (json == null || json.isBlank()) {
            return new EspnSummaryDto(eventId, java.util.List.of(), 0);
        }
        try {
            JsonObject root = JsonParser.parseObject(json);
            return EspnSummaryDto.fromJson(eventId, root);
        } catch (RuntimeException e) {
            throw new EspnApiException("Resposta JSON invalida da ESPN para resumo " + eventId, e);
        }
    }

    public Optional<EspnSummaryDto> buscarResumoSeguro(String eventId) {
        try {
            return Optional.of(buscarResumo(eventId));
        } catch (EspnApiException e) {
            System.err.println("[ESPN Client] Fallback ativado ao buscar resumo da partida " + eventId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private String executarRequisicao(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "GigaBet/1.0 (Integration; Brasileirao)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                return response.body();
            }

            throw new EspnApiException("Resposta inesperada da ESPN: HTTP " + status + " para URL " + url);
        } catch (EspnApiException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new EspnApiException("Timeout ao conectar com a API da ESPN (" + timeout.toSeconds() + "s): " + e.getMessage(), e);
        } catch (IOException e) {
            throw new EspnApiException("Erro de conexao/rede ao acessar ESPN (" + url + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EspnApiException("Requisicao interrompida ao acessar ESPN: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EspnApiException("Falha inesperada ao comunicar com ESPN: " + e.getMessage(), e);
        }
    }
}
