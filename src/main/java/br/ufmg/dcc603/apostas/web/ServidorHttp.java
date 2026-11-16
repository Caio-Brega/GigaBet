package br.ufmg.dcc603.apostas.web;

import br.ufmg.dcc603.apostas.Sistema;
import br.ufmg.dcc603.apostas.aplicacao.AutenticacaoService;
import br.ufmg.dcc603.apostas.aplicacao.ComprovanteAposta;
import br.ufmg.dcc603.apostas.aplicacao.CotacaoAposta;
import br.ufmg.dcc603.apostas.aplicacao.FiltroHistorico;
import br.ufmg.dcc603.apostas.aplicacao.ItemHistorico;
import br.ufmg.dcc603.apostas.aplicacao.PedidoAposta;
import br.ufmg.dcc603.apostas.aplicacao.PedidoCadastro;
import br.ufmg.dcc603.apostas.aplicacao.RegraNegocioException;
import br.ufmg.dcc603.apostas.aplicacao.RelatorioApostador;
import br.ufmg.dcc603.apostas.aplicacao.RelatorioPartida;
import br.ufmg.dcc603.apostas.aplicacao.ResumoApuracao;
import br.ufmg.dcc603.apostas.aplicacao.Sessao;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Equipe;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.RegistroAuditoria;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServidorHttp {
    private final Sistema sistema;
    private final int porta;
    private HttpServer server;

    public ServidorHttp(Sistema sistema, int porta) {
        this.sistema = sistema;
        this.porta = porta;
    }

    public static ServidorHttp iniciar(Sistema sistema, int porta) {
        ServidorHttp servidor = new ServidorHttp(sistema, porta);
        servidor.iniciar();
        return servidor;
    }

    public static void main(String[] args) {
        Sistema sistema = Sistema.abrir(Clock.systemUTC());
        int porta = 8080;
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        ServidorHttp servidor = iniciar(sistema, porta);
        servidor.semearDadosIniciais();
        System.out.println("Servidor HTTP GigaBet rodando na porta " + porta);
    }

    public void iniciar() {
        try {
            server = HttpServer.create(new InetSocketAddress(porta), 0);
            server.createContext("/api/", new ApiHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("[GigaBet API] Servidor HTTP iniciado em http://localhost:" + porta);
        } catch (IOException e) {
            System.err.println("[GigaBet API] Erro ao iniciar servidor HTTP na porta " + porta + ": " + e.getMessage());
        }
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
        }
    }

    public void semearDadosIniciais() {
        try {
            Sessao admin = sistema.autenticacao().autenticar(
                    AutenticacaoService.EMAIL_ADMIN, AutenticacaoService.SENHA_ADMIN);
            sistema.partidas().semearDemonstracaoSeVazio(admin);
        } catch (RuntimeException e) {
            System.err.println("[GigaBet API] Falha ao semear dados: " + e.getMessage());
        }
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS Pre-flight and headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String body = lerCorpo(exchange.getRequestBody());
            Map<String, String> json = parseJson(body);
            Sessao sessao = extrairSessao(exchange);

            try {
                if (path.equals("/api/status") && method.equals("GET")) {
                    responderJson(exchange, 200, "{\"status\":\"online\",\"app\":\"GigaBet API\"}");
                } else if (path.equals("/api/auth/cadastro") && method.equals("POST")) {
                    String nome = json.get("nome");
                    String cpf = json.get("cpf");
                    String email = json.get("email");
                    String senha = json.get("senha");
                    boolean aceitou = Boolean.parseBoolean(json.getOrDefault("aceitouTermo", "false"));
                    Apostador apostador = sistema.autenticacao().cadastrar(new PedidoCadastro(nome, cpf, email, senha, aceitou));
                    Sessao novaSessao = sistema.autenticacao().autenticar(email, senha);
                    responderJson(exchange, 201, jsonApostador(apostador, novaSessao.token()));
                } else if (path.equals("/api/auth/login") && method.equals("POST")) {
                    String email = json.get("email");
                    String senha = json.get("senha");
                    Sessao login = sistema.autenticacao().autenticar(email, senha);
                    Usuario usuario = sistema.autenticacao().exigir(login);
                    responderJson(exchange, 200, jsonUsuario(usuario, login.token()));
                } else if (path.equals("/api/auth/me") && method.equals("GET")) {
                    exigirSessao(sessao);
                    Usuario usuario = sistema.autenticacao().exigir(sessao);
                    responderJson(exchange, 200, jsonUsuario(usuario, sessao.token()));
                } else if (path.equals("/api/auth/sessao") && method.equals("DELETE")) {
                    exigirSessao(sessao);
                    sistema.autenticacao().encerrar(sessao);
                    responderJson(exchange, 200, "{\"sucesso\":true}");
                } else if (path.equals("/api/auth/deposito") && method.equals("POST")) {
                    exigirSessao(sessao);
                    BigDecimal valor = new BigDecimal(json.getOrDefault("valor", "0.00"));
                    sistema.autenticacao().depositar(sessao, valor);
                    Apostador apostador = sistema.autenticacao().exigirApostador(sessao);
                    responderJson(exchange, 200, "{\"sucesso\":true,\"novoSaldo\":\"" + apostador.saldo() + "\"}");
                } else if (path.equals("/api/partidas") && method.equals("GET")) {
                    List<Partida> partidas = sistema.partidas().listarCatalogo();
                    responderJson(exchange, 200, jsonListaPartidas(partidas));
                } else if (path.equals("/api/partidas") && method.equals("POST")) {
                    exigirSessao(sessao);
                    String mandante = json.get("mandante");
                    String visitante = json.get("visitante");
                    Instant inicio = Instant.parse(json.get("inicio"));
                    Partida partida = sistema.partidas().cadastrarPartida(sessao, mandante, visitante, inicio);
                    responderJson(exchange, 201, jsonPartida(partida));
                } else if (path.matches("^/api/partidas/\\d+/resultado$") && method.equals("POST")) {
                    exigirSessao(sessao);
                    long id = Long.parseLong(path.split("/")[3]);
                    int golsMandante = Integer.parseInt(json.get("golsMandante"));
                    int golsVisitante = Integer.parseInt(json.get("golsVisitante"));
                    int cartoes = Integer.parseInt(json.get("cartoesAmarelos"));
                    ResumoApuracao resumo = sistema.partidas().registrarResultado(sessao, id, golsMandante, golsVisitante, cartoes);
                    responderJson(exchange, 200, "{\"sucesso\":true,\"apostasApuradas\":" + resumo.apostasApuradas()
                            + ",\"vencedoras\":" + resumo.vencedoras() + ",\"perdedoras\":" + resumo.perdedoras() + "}");
                } else if (path.equals("/api/equipes") && method.equals("GET")) {
                    List<Equipe> equipes = sistema.partidas().listarEquipes();
                    responderJson(exchange, 200, jsonListaEquipes(equipes));
                } else if (path.equals("/api/equipes") && method.equals("POST")) {
                    exigirSessao(sessao);
                    String nome = json.get("nome");
                    Equipe equipe = sistema.partidas().cadastrarEquipe(sessao, nome);
                    responderJson(exchange, 201, "{\"id\":" + equipe.id() + ",\"nome\":\"" + escape(equipe.nome()) + "\"}");
                } else if (path.equals("/api/apostas/cotar") && method.equals("POST")) {
                    exigirSessao(sessao);
                    PedidoAposta pedido = parsePedido(json);
                    CotacaoAposta cotacao = sistema.apostas().cotar(sessao, pedido);
                    responderJson(exchange, 200, "{\"probabilidade\":\"" + cotacao.probabilidade()
                            + "\",\"premioPotencial\":\"" + cotacao.premioPotencial()
                            + "\",\"saldoAtual\":\"" + cotacao.saldoAtual() + "\"}");
                } else if (path.equals("/api/apostas/confirmar") && method.equals("POST")) {
                    exigirSessao(sessao);
                    PedidoAposta pedido = parsePedido(json);
                    ComprovanteAposta comprovante = sistema.apostas().confirmar(sessao, pedido);
                    responderJson(exchange, 201, "{\"apostaId\":" + comprovante.apostaId()
                            + ",\"partidaId\":" + comprovante.partidaId()
                            + ",\"descricaoPartida\":\"" + escape(comprovante.partida()) + "\""
                            + ",\"tipo\":\"" + comprovante.tipo() + "\""
                            + ",\"previsao\":\"" + escape(comprovante.previsao()) + "\""
                            + ",\"valor\":\"" + comprovante.valor() + "\""
                            + ",\"premioPotencial\":\"" + comprovante.premioPotencial() + "\""
                            + ",\"saldoRestante\":\"" + comprovante.saldoRestante() + "\""
                            + ",\"status\":\"" + comprovante.status() + "\"}");
                } else if (path.equals("/api/apostas/historico") && method.equals("GET")) {
                    exigirSessao(sessao);
                    List<ItemHistorico> itens = sistema.apostas().consultarHistorico(sessao, FiltroHistorico.nenhum());
                    responderJson(exchange, 200, jsonHistorico(itens));
                } else if (path.matches("^/api/relatorios/partida/\\d+$") && method.equals("GET")) {
                    exigirSessao(sessao);
                    long id = Long.parseLong(path.substring("/api/relatorios/partida/".length()));
                    RelatorioPartida r = sistema.relatorios().porPartida(sessao, id);
                    responderJson(exchange, 200, jsonRelatorioPartida(r));
                } else if (path.startsWith("/api/relatorios/apostador") && method.equals("GET")) {
                    exigirSessao(sessao);
                    String busca = extrairParametro(exchange.getRequestURI().getQuery(), "busca");
                    RelatorioApostador r = sistema.relatorios().porApostador(sessao, busca);
                    responderJson(exchange, 200, jsonRelatorioApostador(r));
                } else if (path.equals("/api/auditoria") && method.equals("GET")) {
                    exigirSessao(sessao);
                    sistema.autenticacao().exigirAdministrador(sessao);
                    List<RegistroAuditoria> logs = sistema.auditoria().listar();
                    responderJson(exchange, 200, jsonAuditoria(logs));
                } else {
                    responderJson(exchange, 404, "{\"erro\":\"Rota nao encontrada: " + path + "\"}");
                }
            } catch (RegraNegocioException e) {
                responderJson(exchange, 400, "{\"erro\":\"" + escape(e.getMessage()) + "\",\"codigo\":\"" + e.codigo().name() + "\"}");
            } catch (Exception e) {
                responderJson(exchange, 500, "{\"erro\":\"" + escape(e.getMessage() != null ? e.getMessage() : e.toString()) + "\"}");
            }
        }
    }

    private Sessao extrairSessao(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return sistema.autenticacao().localizar(auth.substring(7).trim());
        }
        return null;
    }

    private void exigirSessao(Sessao sessao) {
        if (sessao == null) {
            throw new RegraNegocioException(br.ufmg.dcc603.apostas.aplicacao.CodigoRegra.SESSAO_INVALIDA);
        }
    }

    private PedidoAposta parsePedido(Map<String, String> json) {
        long partidaId = Long.parseLong(json.get("partidaId"));
        String tipo = json.get("tipo");
        BigDecimal valor = new BigDecimal(json.get("valor"));
        if ("EQUIPE_VENCEDORA".equalsIgnoreCase(tipo)) {
            Desfecho desfecho = Desfecho.valueOf(json.get("desfecho").toUpperCase());
            return PedidoAposta.vencedor(partidaId, desfecho, valor);
        } else if ("PLACAR_EXATO".equalsIgnoreCase(tipo)) {
            int gm = Integer.parseInt(json.get("golsMandante"));
            int gv = Integer.parseInt(json.get("golsVisitante"));
            return PedidoAposta.placar(partidaId, gm, gv, valor);
        } else if ("CARTOES_AMARELOS".equalsIgnoreCase(tipo)) {
            int cartoes = Integer.parseInt(json.get("cartoesAmarelos"));
            return PedidoAposta.cartoes(partidaId, cartoes, valor);
        }
        throw new RegraNegocioException(br.ufmg.dcc603.apostas.aplicacao.CodigoRegra.PREVISAO_INVALIDA);
    }

    private String jsonUsuario(Usuario u, String token) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(u.id());
        sb.append(",\"nome\":\"").append(escape(u.nome())).append("\"");
        sb.append(",\"email\":\"").append(escape(u.email())).append("\"");
        sb.append(",\"perfil\":\"").append(u.perfil().name()).append("\"");
        if (u instanceof Apostador a) {
            sb.append(",\"cpf\":\"").append(escape(a.cpf())).append("\"");
            sb.append(",\"saldo\":\"").append(a.saldo()).append("\"");
        }
        if (token != null) {
            sb.append(",\"token\":\"").append(token).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String jsonApostador(Apostador a, String token) {
        return jsonUsuario(a, token);
    }

    private String jsonPartida(Partida p) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(p.id());
        sb.append(",\"descricao\":\"").append(escape(p.descricao())).append("\"");
        sb.append(",\"mandante\":\"").append(escape(p.mandante().nome())).append("\"");
        sb.append(",\"visitante\":\"").append(escape(p.visitante().nome())).append("\"");
        sb.append(",\"inicio\":\"").append(p.inicio().toString()).append("\"");
        sb.append(",\"status\":\"").append(p.status().name()).append("\"");
        if (p.resultado() != null) {
            sb.append(",\"golsMandante\":").append(p.resultado().golsMandante());
            sb.append(",\"golsVisitante\":").append(p.resultado().golsVisitante());
            sb.append(",\"cartoesAmarelos\":").append(p.resultado().cartoesAmarelos());
        }
        sb.append("}");
        return sb.toString();
    }

    private String jsonListaPartidas(List<Partida> partidas) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < partidas.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonPartida(partidas.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonListaEquipes(List<Equipe> equipes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < equipes.size(); i++) {
            if (i > 0) sb.append(",");
            Equipe e = equipes.get(i);
            sb.append("{\"id\":").append(e.id()).append(",\"nome\":\"").append(escape(e.nome())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonHistorico(List<ItemHistorico> itens) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) sb.append(",");
            ItemHistorico item = itens.get(i);
            sb.append("{");
            sb.append("\"apostaId\":").append(item.apostaId());
            sb.append(",\"partidaId\":").append(item.partidaId());
            sb.append(",\"partida\":\"").append(escape(item.partida())).append("\"");
            sb.append(",\"tipo\":\"").append(item.tipo().name()).append("\"");
            sb.append(",\"previsao\":\"").append(escape(item.previsao())).append("\"");
            sb.append(",\"valor\":\"").append(item.valor()).append("\"");
            sb.append(",\"probabilidade\":\"").append(item.probabilidade()).append("\"");
            sb.append(",\"premioPotencial\":\"").append(item.premioPotencial()).append("\"");
            sb.append(",\"status\":\"").append(item.status().name()).append("\"");
            sb.append(",\"criadaEm\":\"").append(item.criadaEm().toString()).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonRelatorioPartida(RelatorioPartida r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"partidaId\":").append(r.partidaId());
        sb.append(",\"partida\":\"").append(escape(r.partida())).append("\"");
        sb.append(",\"quantidade\":").append(r.quantidade());
        sb.append(",\"valorTotal\":\"").append(r.valorTotal()).append("\"");
        sb.append(",\"totalPremiosPagos\":\"").append(r.totalPremiosPagos()).append("\"");
        sb.append(",\"apostas\":[");
        for (int i = 0; i < r.apostas().size(); i++) {
            if (i > 0) sb.append(",");
            var l = r.apostas().get(i);
            sb.append("{\"id\":").append(l.apostaId())
              .append(",\"apostador\":\"").append(escape(l.apostador())).append("\"")
              .append(",\"tipo\":\"").append(l.tipo()).append("\"")
              .append(",\"previsao\":\"").append(escape(l.previsao())).append("\"")
              .append(",\"valor\":\"").append(l.valor()).append("\"")
              .append(",\"premioPotencial\":\"").append(l.premioPotencial()).append("\"")
              .append(",\"status\":\"").append(l.status()).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String jsonRelatorioApostador(RelatorioApostador r) {
        return "{\"apostadorId\":" + r.apostadorId()
                + ",\"nome\":\"" + escape(r.nome()) + "\""
                + ",\"cpf\":\"" + escape(r.cpf()) + "\""
                + ",\"quantidade\":" + r.quantidade()
                + ",\"valorTotal\":\"" + r.valorTotal() + "\""
                + ",\"abertas\":" + r.abertas()
                + ",\"vencedoras\":" + r.vencedoras()
                + ",\"perdedoras\":" + r.perdedoras()
                + ",\"totalPremiosRecebidos\":\"" + r.totalPremiosRecebidos() + "\"}";
    }

    private String jsonAuditoria(List<RegistroAuditoria> logs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < logs.size(); i++) {
            if (i > 0) sb.append(",");
            var l = logs.get(i);
            sb.append("{\"id\":").append(l.id())
              .append(",\"usuarioId\":").append(l.usuarioId())
              .append(",\"tipo\":\"").append(l.tipo().name()).append("\"")
              .append(",\"detalhes\":\"").append(escape(l.descricao())).append("\"")
              .append(",\"quando\":\"").append(l.quando().toString()).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String lerCorpo(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([truefalsetrueFALSENULLnull0-9.-]+))");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String chave = m.group(1);
            String valor = m.group(2) != null ? m.group(2) : m.group(3);
            map.put(chave, valor);
        }
        return map;
    }

    private String extrairParametro(String query, String chave) {
        if (query == null) return "";
        for (String par : query.split("&")) {
            String[] kv = par.split("=");
            if (kv.length == 2 && kv[0].equals(chave)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private String escape(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
