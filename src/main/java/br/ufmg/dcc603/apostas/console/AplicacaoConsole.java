package br.ufmg.dcc603.apostas.console;

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
import br.ufmg.dcc603.apostas.dominio.Desfecho;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.Perfil;
import br.ufmg.dcc603.apostas.dominio.TipoPrevisao;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Menu de texto. Usa o mesmo armazenamento da API: Postgres, ou memoria
 * se GIGABET_ARMAZENAMENTO=memoria.
 */
public final class AplicacaoConsole {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Sistema sistema;
    private final Scanner entrada;
    private Sessao sessao;

    public AplicacaoConsole(Sistema sistema, Scanner entrada) {
        this.sistema = sistema;
        this.entrada = entrada;
    }

    public static void main(String[] args) {
        Sistema sistema = Sistema.abrir(Clock.systemUTC());
        System.out.println("Gestao de Apostas Esportivas (GigaBet)");
        System.out.println("Administrador inicial: " + AutenticacaoService.EMAIL_ADMIN
                + " / " + AutenticacaoService.SENHA_ADMIN);
        br.ufmg.dcc603.apostas.web.ServidorHttp servidor = br.ufmg.dcc603.apostas.web.ServidorHttp.iniciar(sistema, 8080);
        servidor.semearDadosIniciais();
        servidor.iniciarSincronizacaoAutomatica();
        new AplicacaoConsole(sistema, new Scanner(System.in)).executar();
    }

    public void executar() {
        boolean ativo = true;
        while (ativo && entrada.hasNextLine()) {
            imprimirMenu();
            String opcao = entrada.nextLine().trim();
            try {
                ativo = executarOpcao(opcao);
            } catch (RegraNegocioException e) {
                System.out.println("Nao foi possivel concluir: " + e.getMessage());
            } catch (DateTimeParseException | NumberFormatException e) {
                System.out.println("Entrada em formato invalido.");
            }
        }
    }

    private boolean executarOpcao(String opcao) {
        return switch (opcao) {
            case "1" -> {
                cadastrar();
                yield true;
            }
            case "2" -> {
                sessao = sistema.autenticacao().autenticar(ler("E-mail"), ler("Senha"));
                System.out.println("Sessao iniciada como " + sessao.perfil() + ".");
                yield true;
            }
            case "3" -> {
                exigirSessao();
                sistema.autenticacao().depositar(sessao, new BigDecimal(ler("Valor do deposito simulado")));
                System.out.println("Saldo atual: " + sistema.autenticacao().exigirApostador(sessao).saldo());
                yield true;
            }
            case "4" -> {
                exigirSessao();
                var equipe = sistema.partidas().cadastrarEquipe(sessao, ler("Nome da equipe"));
                System.out.println("Equipe " + equipe.id() + " cadastrada.");
                yield true;
            }
            case "5" -> {
                exigirSessao();
                Partida partida = sistema.partidas().cadastrarPartida(
                        sessao, ler("Mandante"), ler("Visitante"), lerInicio());
                System.out.println("Partida " + partida.id() + " agendada: " + descrever(partida));
                yield true;
            }
            case "6" -> {
                exigirSessao();
                listar();
                yield true;
            }
            case "7" -> {
                exigirSessao();
                apostar();
                yield true;
            }
            case "8" -> {
                exigirSessao();
                for (ItemHistorico item : sistema.apostas().consultarHistorico(sessao, FiltroHistorico.nenhum())) {
                    System.out.println("#" + item.apostaId() + " " + item.partida() + " | " + item.previsao()
                            + " | " + item.valor() + " | premio " + item.premioPotencial()
                            + " | " + item.status());
                }
                yield true;
            }
            case "9" -> {
                exigirSessao();
                ResumoApuracao resumo = sistema.partidas().registrarResultado(
                        sessao,
                        Long.parseLong(ler("Id da partida")),
                        Integer.parseInt(ler("Gols do mandante")),
                        Integer.parseInt(ler("Gols do visitante")),
                        Integer.parseInt(ler("Cartoes amarelos")));
                System.out.println("Apuradas " + resumo.apostasApuradas()
                        + " | vencedoras " + resumo.vencedoras()
                        + " | perdedoras " + resumo.perdedoras());
                yield true;
            }
            case "10" -> {
                exigirSessao();
                RelatorioPartida relatorio = sistema.relatorios().porPartida(
                        sessao, Long.parseLong(ler("Id da partida")));
                System.out.println(relatorio.partida() + ": " + relatorio.quantidade()
                        + " apostas, volume " + relatorio.valorTotal()
                        + ", premios pagos " + relatorio.totalPremiosPagos());
                yield true;
            }
            case "11" -> {
                exigirSessao();
                RelatorioApostador relatorio = sistema.relatorios().porApostador(sessao, ler("Nome ou CPF"));
                System.out.println(relatorio.nome() + ": " + relatorio.quantidade()
                        + " apostas, abertas " + relatorio.abertas()
                        + ", vencedoras " + relatorio.vencedoras()
                        + ", premios " + relatorio.totalPremiosRecebidos());
                yield true;
            }
            case "0" -> false;
            default -> {
                System.out.println("Opcao desconhecida.");
                yield true;
            }
        };
    }

    private void cadastrar() {
        String nome = ler("Nome");
        String cpf = ler("CPF");
        String email = ler("E-mail");
        String senha = ler("Senha");
        String termo = ler("Aceita o termo de tratamento de dados? (s/n)");
        sistema.autenticacao().cadastrar(new PedidoCadastro(nome, cpf, email, senha, termo.equalsIgnoreCase("s")));
        System.out.println("Cadastro concluido. Saldo inicial: 0.00");
    }

    private void listar() {
        List<Partida> partidas = sessao.perfil() == Perfil.APOSTADOR
                ? sistema.partidas().listarDisponiveis(sessao)
                : sistema.partidas().listarTodas(sessao);
        if (partidas.isEmpty()) {
            System.out.println("Nenhuma partida para exibir.");
            return;
        }
        for (Partida partida : partidas) {
            System.out.println(descrever(partida));
        }
    }

    private void apostar() {
        long partidaId = Long.parseLong(ler("Id da partida"));
        System.out.println("Tipo: 1 equipe vencedora, 2 placar exato, 3 cartoes amarelos");
        String tipo = ler("Tipo");
        PedidoAposta pedido = switch (tipo) {
            case "1" -> {
                System.out.println("Desfecho: 1 mandante, 2 empate, 3 visitante");
                Desfecho desfecho = switch (ler("Desfecho")) {
                    case "1" -> Desfecho.MANDANTE;
                    case "2" -> Desfecho.EMPATE;
                    case "3" -> Desfecho.VISITANTE;
                    default -> throw new RegraNegocioException(
                            br.ufmg.dcc603.apostas.aplicacao.CodigoRegra.PREVISAO_INVALIDA);
                };
                yield PedidoAposta.vencedor(partidaId, desfecho, new BigDecimal(ler("Valor")));
            }
            case "2" -> PedidoAposta.placar(
                    partidaId,
                    Integer.parseInt(ler("Gols do mandante")),
                    Integer.parseInt(ler("Gols do visitante")),
                    new BigDecimal(ler("Valor")));
            case "3" -> PedidoAposta.cartoes(
                    partidaId, Integer.parseInt(ler("Cartoes amarelos (0 a 10)")), new BigDecimal(ler("Valor")));
            default -> throw new RegraNegocioException(
                    br.ufmg.dcc603.apostas.aplicacao.CodigoRegra.PREVISAO_INVALIDA);
        };
        CotacaoAposta cotacao = sistema.apostas().cotar(sessao, pedido);
        System.out.println("Probabilidade " + cotacao.probabilidade()
                + " | premio potencial " + cotacao.premioPotencial()
                + " | saldo " + cotacao.saldoAtual());
        if (!ler("Confirmar aposta? (s/n)").equalsIgnoreCase("s")) {
            System.out.println("Aposta cancelada. Nada foi debitado.");
            return;
        }
        ComprovanteAposta comprovante = sistema.apostas().confirmar(sessao, pedido);
        System.out.println("Aposta " + comprovante.apostaId() + " registrada. Saldo: "
                + comprovante.saldoRestante());
    }

    private String descrever(Partida partida) {
        String quando = DATA.format(LocalDateTime.ofInstant(partida.inicio(), ZONA));
        return "#" + partida.id() + " " + partida.descricao() + " | " + quando
                + " | " + partida.status()
                + " | tipos " + TipoPrevisao.EQUIPE_VENCEDORA + ", "
                + TipoPrevisao.PLACAR_EXATO + ", " + TipoPrevisao.CARTOES_AMARELOS;
    }

    private Instant lerInicio() {
        LocalDateTime local = LocalDateTime.parse(ler("Inicio (dd/MM/yyyy HH:mm)"), DATA);
        return local.atZone(ZONA).toInstant();
    }

    private void imprimirMenu() {
        System.out.println();
        System.out.println("1 Cadastrar apostador  2 Entrar  3 Depositar saldo simulado");
        System.out.println("4 Cadastrar equipe     5 Cadastrar partida  6 Listar partidas");
        System.out.println("7 Apostar              8 Meu historico      9 Registrar resultado");
        System.out.println("10 Relatorio da partida  11 Relatorio do apostador  0 Sair");
        System.out.print("> ");
    }

    private void exigirSessao() {
        if (sessao == null) {
            throw new RegraNegocioException(br.ufmg.dcc603.apostas.aplicacao.CodigoRegra.SESSAO_INVALIDA);
        }
    }

    private String ler(String rotulo) {
        System.out.print(rotulo + ": ");
        return entrada.nextLine().trim();
    }
}
