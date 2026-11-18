package br.ufmg.dcc603.apostas;

import br.ufmg.dcc603.apostas.aplicacao.ApostaService;
import br.ufmg.dcc603.apostas.aplicacao.ApuracaoService;
import br.ufmg.dcc603.apostas.aplicacao.AuditoriaService;
import br.ufmg.dcc603.apostas.aplicacao.AutenticacaoService;
import br.ufmg.dcc603.apostas.aplicacao.CalculadoraPremio;
import br.ufmg.dcc603.apostas.aplicacao.CalculadoraProbabilidade;
import br.ufmg.dcc603.apostas.aplicacao.ParametrosCalculo;
import br.ufmg.dcc603.apostas.aplicacao.PartidaService;
import br.ufmg.dcc603.apostas.aplicacao.RelatorioService;
import br.ufmg.dcc603.apostas.aplicacao.SenhaHasher;
import br.ufmg.dcc603.apostas.aplicacao.SenhaHasherBcrypt;
import br.ufmg.dcc603.apostas.aplicacao.SenhaHasherSha256;
import br.ufmg.dcc603.apostas.aplicacao.SincronizacaoEspnService;
import br.ufmg.dcc603.apostas.integracao.espn.EspnBrasileiraoClient;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.AuditoriaRepository;
import br.ufmg.dcc603.apostas.persistencia.EquipeRepository;
import br.ufmg.dcc603.apostas.persistencia.PartidaRepository;
import br.ufmg.dcc603.apostas.persistencia.SessaoRepository;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;
import br.ufmg.dcc603.apostas.persistencia.jdbc.AuditoriaRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.jdbc.Banco;
import br.ufmg.dcc603.apostas.persistencia.jdbc.CpfCofre;
import br.ufmg.dcc603.apostas.persistencia.jdbc.EquipeRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.jdbc.PartidaRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.jdbc.SessaoRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.jdbc.UsuarioRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.jdbc.ApostaRepositoryJdbc;
import br.ufmg.dcc603.apostas.persistencia.memoria.ApostaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.AuditoriaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.EquipeRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.EstadoMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.PartidaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.SessaoRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.UsuarioRepositoryMemoria;

import java.time.Clock;

public final class Sistema {
    private final AutenticacaoService autenticacao;
    private final PartidaService partidas;
    private final ApostaService apostas;
    private final RelatorioService relatorios;
    private final AuditoriaService auditoria;
    private final SincronizacaoEspnService sincronizacaoEspn;

    private Sistema(
            AutenticacaoService autenticacao,
            PartidaService partidas,
            ApostaService apostas,
            RelatorioService relatorios,
            AuditoriaService auditoria,
            SincronizacaoEspnService sincronizacaoEspn) {
        this.autenticacao = autenticacao;
        this.partidas = partidas;
        this.apostas = apostas;
        this.relatorios = relatorios;
        this.auditoria = auditoria;
        this.sincronizacaoEspn = sincronizacaoEspn;
    }

    public static Sistema criar(Clock relogio) {
        return criar(relogio, new EspnBrasileiraoClient());
    }

    public static Sistema criar(Clock relogio, EspnBrasileiraoClient espnClient) {
        EstadoMemoria estado = new EstadoMemoria();
        return criar(relogio, estado, new ApostaRepositoryMemoria(estado), new SenhaHasherSha256(), espnClient);
    }

    public static Sistema criar(Clock relogio, EstadoMemoria estado, ApostaRepository apostas, SenhaHasher hasher) {
        return criar(relogio, estado, apostas, hasher, new EspnBrasileiraoClient());
    }

    public static Sistema criar(
            Clock relogio,
            EstadoMemoria estado,
            ApostaRepository apostas,
            SenhaHasher hasher,
            EspnBrasileiraoClient espnClient) {
        return montar(
                relogio,
                estado,
                apostas,
                hasher,
                new UsuarioRepositoryMemoria(estado),
                new PartidaRepositoryMemoria(estado),
                new EquipeRepositoryMemoria(estado),
                new AuditoriaRepositoryMemoria(estado),
                new SessaoRepositoryMemoria(),
                espnClient);
    }

    public static Sistema abrir(Clock relogio) {
        String modo = System.getenv().getOrDefault("GIGABET_ARMAZENAMENTO", "postgres");
        if ("memoria".equalsIgnoreCase(modo)) {
            return criar(relogio);
        }
        String url = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5434/gigabet");
        String usuario = System.getenv().getOrDefault("JDBC_USER", "gigabet");
        String senha = System.getenv().getOrDefault("JDBC_PASSWORD", "gigabet");
        String chaveCpf = System.getenv().getOrDefault("CPF_CHAVE", "gigabet-dev-cpf-chave");
        return criarJdbc(relogio, url, usuario, senha, chaveCpf);
    }

    public static Sistema criarJdbc(Clock relogio, String url, String usuario, String senha, String chaveCpf) {
        Banco banco = new Banco(url, usuario, senha);
        banco.instalarEsquema();
        CpfCofre cpf = new CpfCofre(chaveCpf);
        EquipeRepository equipes = new EquipeRepositoryJdbc(banco);
        return montar(
                relogio,
                banco,
                new ApostaRepositoryJdbc(banco),
                new SenhaHasherBcrypt(),
                new UsuarioRepositoryJdbc(banco, cpf),
                new PartidaRepositoryJdbc(banco, equipes),
                equipes,
                new AuditoriaRepositoryJdbc(banco),
                new SessaoRepositoryJdbc(banco));
    }

    private static Sistema montar(
            Clock relogio,
            UnidadeDeTrabalho unidade,
            ApostaRepository apostas,
            SenhaHasher hasher,
            UsuarioRepository usuarios,
            PartidaRepository partidas,
            EquipeRepository equipes,
            AuditoriaRepository auditoriaRepositorio,
            SessaoRepository sessoes) {
        return montar(relogio, unidade, apostas, hasher, usuarios, partidas, equipes, auditoriaRepositorio, sessoes, new EspnBrasileiraoClient());
    }

    private static Sistema montar(
            Clock relogio,
            UnidadeDeTrabalho unidade,
            ApostaRepository apostas,
            SenhaHasher hasher,
            UsuarioRepository usuarios,
            PartidaRepository partidas,
            EquipeRepository equipes,
            AuditoriaRepository auditoriaRepositorio,
            SessaoRepository sessoes,
            EspnBrasileiraoClient espnClient) {
        AuditoriaService auditoria = new AuditoriaService(auditoriaRepositorio, unidade, relogio);
        AutenticacaoService autenticacao = new AutenticacaoService(
                usuarios, unidade, hasher, auditoria, sessoes, relogio);
        ApuracaoService apuracao = new ApuracaoService(apostas, usuarios);
        PartidaService partidaService = new PartidaService(
                equipes, partidas, unidade, autenticacao, apuracao, auditoria, relogio);
        CalculadoraProbabilidade probabilidade = CalculadoraProbabilidade.padrao();
        CalculadoraPremio premio = new CalculadoraPremio(ParametrosCalculo.PADRAO);
        ApostaService apostaService = new ApostaService(
                apostas, partidas, usuarios, unidade, autenticacao, partidaService,
                probabilidade, premio, auditoria, relogio);
        RelatorioService relatorios = new RelatorioService(autenticacao, apostas, partidas, usuarios);
        SincronizacaoEspnService sincronizacaoEspn = new SincronizacaoEspnService(
                espnClient, equipes, partidas, unidade, autenticacao, apuracao, auditoria, relogio);
        autenticacao.semearAdministrador();
        return new Sistema(autenticacao, partidaService, apostaService, relatorios, auditoria, sincronizacaoEspn);
    }

    public AutenticacaoService autenticacao() {
        return autenticacao;
    }

    public PartidaService partidas() {
        return partidas;
    }

    public ApostaService apostas() {
        return apostas;
    }

    public RelatorioService relatorios() {
        return relatorios;
    }

    public AuditoriaService auditoria() {
        return auditoria;
    }

    public SincronizacaoEspnService sincronizacaoEspn() {
        return sincronizacaoEspn;
    }
}
