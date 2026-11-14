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
import br.ufmg.dcc603.apostas.aplicacao.SenhaHasherSha256;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.memoria.ApostaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.AuditoriaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.EquipeRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.EstadoMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.PartidaRepositoryMemoria;
import br.ufmg.dcc603.apostas.persistencia.memoria.UsuarioRepositoryMemoria;

import java.time.Clock;

public final class Sistema {
    private final EstadoMemoria estado;
    private final AutenticacaoService autenticacao;
    private final PartidaService partidas;
    private final ApostaService apostas;
    private final RelatorioService relatorios;
    private final AuditoriaService auditoria;

    private Sistema(
            EstadoMemoria estado,
            AutenticacaoService autenticacao,
            PartidaService partidas,
            ApostaService apostas,
            RelatorioService relatorios,
            AuditoriaService auditoria) {
        this.estado = estado;
        this.autenticacao = autenticacao;
        this.partidas = partidas;
        this.apostas = apostas;
        this.relatorios = relatorios;
        this.auditoria = auditoria;
    }

    public static Sistema criar(Clock relogio) {
        EstadoMemoria estado = new EstadoMemoria();
        return criar(relogio, estado, new ApostaRepositoryMemoria(estado), new SenhaHasherSha256());
    }

    public static Sistema criar(Clock relogio, EstadoMemoria estado, ApostaRepository apostas, SenhaHasher hasher) {
        UsuarioRepositoryMemoria usuarios = new UsuarioRepositoryMemoria(estado);
        PartidaRepositoryMemoria partidas = new PartidaRepositoryMemoria(estado);
        AuditoriaService auditoria = new AuditoriaService(new AuditoriaRepositoryMemoria(estado), estado, relogio);
        AutenticacaoService autenticacao = new AutenticacaoService(usuarios, estado, hasher, auditoria, relogio);
        ApuracaoService apuracao = new ApuracaoService(apostas, usuarios);
        PartidaService partidaService = new PartidaService(
                new EquipeRepositoryMemoria(estado), partidas, estado, autenticacao, apuracao, auditoria, relogio);
        CalculadoraProbabilidade probabilidade = CalculadoraProbabilidade.padrao();
        CalculadoraPremio premio = new CalculadoraPremio(ParametrosCalculo.PADRAO);
        ApostaService apostaService = new ApostaService(
                apostas, partidas, usuarios, estado, autenticacao, partidaService,
                probabilidade, premio, auditoria, relogio);
        RelatorioService relatorios = new RelatorioService(autenticacao, apostas, partidas, usuarios);
        autenticacao.semearAdministrador();
        return new Sistema(estado, autenticacao, partidaService, apostaService, relatorios, auditoria);
    }

    public EstadoMemoria estado() {
        return estado;
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
}
