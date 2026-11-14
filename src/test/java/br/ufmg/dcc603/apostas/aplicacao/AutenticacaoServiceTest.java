package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.Suporte;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.TipoEventoAuditoria;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacaoServiceTest {
    @Test
    void cadastraApostadorComSenhaHasheadaSaldoZeroEAuditoria() {
        Suporte suporte = new Suporte();
        Apostador apostador = suporte.sistema.autenticacao().cadastrar(
                new PedidoCadastro("Ana Lima", "529.982.247-25", "Ana@Email.com", "senha123", true));

        assertEquals("52998224725", apostador.cpf());
        assertEquals("ana@email.com", apostador.email());
        assertEquals(0, apostador.saldo().compareTo(new java.math.BigDecimal("0.00")));
        assertFalse(apostador.senhaHash().contains("senha123"));
        assertTrue(new SenhaHasherSha256().confere("senha123", apostador.senhaHash())
                || suporte.sistema.autenticacao().autenticar("ANA@email.com", "senha123") != null);
        assertTrue(suporte.sistema.auditoria().listar().stream()
                .anyMatch(registro -> registro.tipo() == TipoEventoAuditoria.CADASTRO_APOSTADOR));
    }

    @Test
    void recusaTermoDuplicidadeESenhaCurta() {
        Suporte suporte = new Suporte();
        assertEquals(CodigoRegra.TERMO_NAO_ACEITO, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.autenticacao().cadastrar(
                        new PedidoCadastro("Ana", "52998224725", "ana@email.com", "senha123", false))).codigo());
        suporte.apostador("Ana Lima", "ana@email.com", "52998224725", null);
        assertEquals(CodigoRegra.EMAIL_DUPLICADO, assertThrows(RegraNegocioException.class,
                () -> suporte.apostador("Outra", "ana@email.com", "39053344705", null)).codigo());
        assertEquals(CodigoRegra.CPF_DUPLICADO, assertThrows(RegraNegocioException.class,
                () -> suporte.apostador("Outra", "outra@email.com", "529.982.247-25", null)).codigo());
        assertEquals(CodigoRegra.DADOS_INVALIDOS, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.autenticacao().cadastrar(
                        new PedidoCadastro("Ana", "11144477735", "curta@email.com", "123", true))).codigo());
    }

    @Test
    void bloqueiaNaQuintaFalhaELiberaDepoisDoPrazo() {
        Suporte suporte = new Suporte();
        suporte.apostador("Ana Lima", "ana@email.com", "52998224725", null);
        for (int i = 0; i < 4; i++) {
            assertEquals(CodigoRegra.CREDENCIAIS_INVALIDAS, assertThrows(RegraNegocioException.class,
                    () -> suporte.sistema.autenticacao().autenticar("ana@email.com", "errada")).codigo());
        }
        assertEquals(CodigoRegra.CONTA_BLOQUEADA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.autenticacao().autenticar("ana@email.com", "errada")).codigo());
        assertEquals(CodigoRegra.CONTA_BLOQUEADA, assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.autenticacao().autenticar("ana@email.com", "senha123")).codigo());

        suporte.relogio.avancar(Duration.ofMinutes(15));
        assertEquals(br.ufmg.dcc603.apostas.dominio.Perfil.APOSTADOR,
                suporte.sistema.autenticacao().autenticar("ana@email.com", "senha123").perfil());
    }

    @Test
    void credencialInexistenteNaoRevelaQualCampoFalhou() {
        Suporte suporte = new Suporte();
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> suporte.sistema.autenticacao().autenticar("ninguem@email.com", "senha123"));
        assertEquals(CodigoRegra.CREDENCIAIS_INVALIDAS, erro.codigo());
        assertFalse(erro.getMessage().toLowerCase().contains("e-mail nao"));
    }
}
