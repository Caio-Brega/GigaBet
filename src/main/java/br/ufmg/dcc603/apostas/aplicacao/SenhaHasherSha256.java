package br.ufmg.dcc603.apostas.aplicacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hash com sal para a implementacao parcial. A arquitetura da Sprint 2
 * preve BCrypt na versao com persistencia real; a interface isola essa troca.
 */
public final class SenhaHasherSha256 implements SenhaHasher {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String gerar(String senha) {
        byte[] sal = new byte[16];
        random.nextBytes(sal);
        return HexFormat.of().formatHex(sal) + "$" + resumo(sal, senha);
    }

    @Override
    public boolean confere(String senha, String hashArmazenado) {
        int separador = hashArmazenado.indexOf('$');
        if (separador <= 0) {
            return false;
        }
        byte[] sal = HexFormat.of().parseHex(hashArmazenado.substring(0, separador));
        byte[] esperado = HexFormat.of().parseHex(hashArmazenado.substring(separador + 1));
        byte[] obtido = HexFormat.of().parseHex(resumo(sal, senha));
        return MessageDigest.isEqual(esperado, obtido);
    }

    private String resumo(byte[] sal, String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sal);
            digest.update(senha.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
