package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Administrador;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import br.ufmg.dcc603.apostas.dominio.Usuario;
import br.ufmg.dcc603.apostas.persistencia.UnidadeDeTrabalho;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class AutenticacaoService {
    public static final String EMAIL_ADMIN = "admin@apostas.local";
    public static final String SENHA_ADMIN = "admin123";

    private final UsuarioRepository usuarios;
    private final UnidadeDeTrabalho unidade;
    private final SenhaHasher hasher;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    private final Map<String, Sessao> sessoes = new HashMap<>();

    public AutenticacaoService(
            UsuarioRepository usuarios,
            UnidadeDeTrabalho unidade,
            SenhaHasher hasher,
            AuditoriaService auditoria,
            Clock relogio) {
        this.usuarios = usuarios;
        this.unidade = unidade;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public void semearAdministrador() {
        if (usuarios.buscarPorEmail(EMAIL_ADMIN).isPresent()) {
            return;
        }
        Administrador administrador = new Administrador(
                unidade.proximoId(), "Administrador", EMAIL_ADMIN, hasher.gerar(SENHA_ADMIN));
        usuarios.salvar(administrador);
    }

    public Apostador cadastrar(PedidoCadastro pedido) {
        if (pedido == null || !pedido.aceitouTermo()) {
            throw new RegraNegocioException(CodigoRegra.TERMO_NAO_ACEITO);
        }
        String nome = textoObrigatorio(pedido.nome(), "Nome obrigatorio.");
        String email = normalizarEmail(pedido.email());
        String cpf = ValidadorCpf.normalizar(pedido.cpf());
        if (pedido.senha() == null || pedido.senha().length() < 6) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "A senha deve ter ao menos 6 caracteres.");
        }
        if (usuarios.buscarPorEmail(email).isPresent()) {
            throw new RegraNegocioException(CodigoRegra.EMAIL_DUPLICADO);
        }
        if (usuarios.buscarPorCpf(cpf).isPresent()) {
            throw new RegraNegocioException(CodigoRegra.CPF_DUPLICADO);
        }
        Apostador apostador = new Apostador(
                unidade.proximoId(), nome, email, hasher.gerar(pedido.senha()), cpf, true);
        usuarios.salvar(apostador);
        auditoria.registrar(TipoEventoAuditoria.CADASTRO_APOSTADOR, apostador.id(),
                "Apostador cadastrado: " + nome);
        return apostador;
    }

    public Sessao autenticar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null) {
            throw new RegraNegocioException(CodigoRegra.CREDENCIAIS_INVALIDAS);
        }
        Usuario usuario = usuarios.buscarPorEmail(email.trim())
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.CREDENCIAIS_INVALIDAS));
        usuario.liberarBloqueioExpirado(relogio.instant());
        if (usuario.bloqueada(relogio.instant())) {
            throw new RegraNegocioException(CodigoRegra.CONTA_BLOQUEADA);
        }
        if (!hasher.confere(senha, usuario.senhaHash())) {
            usuario.registrarFalha(relogio.instant());
            usuarios.salvar(usuario);
            if (usuario.bloqueada(relogio.instant())) {
                throw new RegraNegocioException(CodigoRegra.CONTA_BLOQUEADA);
            }
            throw new RegraNegocioException(CodigoRegra.CREDENCIAIS_INVALIDAS);
        }
        usuario.registrarSucesso();
        usuarios.salvar(usuario);
        Sessao sessao = new Sessao(UUID.randomUUID().toString(), usuario.id(), usuario.perfil());
        sessoes.put(sessao.token(), sessao);
        return sessao;
    }

    public void depositar(Sessao sessao, BigDecimal valor) {
        Apostador apostador = exigirApostador(sessao);
        BigDecimal credito = Dinheiro.positivo(valor);
        apostador.creditar(credito);
        usuarios.salvar(apostador);
        auditoria.registrar(TipoEventoAuditoria.DEPOSITO_SIMULADO, apostador.id(),
                "Deposito simulado de " + credito);
    }

    public Usuario exigir(Sessao sessao) {
        if (sessao == null || !sessoes.containsKey(sessao.token())) {
            throw new RegraNegocioException(CodigoRegra.SESSAO_INVALIDA);
        }
        return usuarios.buscarPorId(sessao.usuarioId())
                .orElseThrow(() -> new RegraNegocioException(CodigoRegra.SESSAO_INVALIDA));
    }

    public Apostador exigirApostador(Sessao sessao) {
        Usuario usuario = exigir(sessao);
        if (usuario instanceof Apostador apostador) {
            return apostador;
        }
        throw new RegraNegocioException(CodigoRegra.ACESSO_NEGADO);
    }

    public Administrador exigirAdministrador(Sessao sessao) {
        Usuario usuario = exigir(sessao);
        if (usuario instanceof Administrador administrador) {
            return administrador;
        }
        throw new RegraNegocioException(CodigoRegra.ACESSO_NEGADO);
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, mensagem);
        }
        return valor.trim();
    }

    private String normalizarEmail(String email) {
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "E-mail invalido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
