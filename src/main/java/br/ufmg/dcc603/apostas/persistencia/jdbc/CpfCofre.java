package br.ufmg.dcc603.apostas.persistencia.jdbc;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CpfCofre {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CpfCofre(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalArgumentException("Chave de CPF obrigatoria");
        }
        this.chave = new SecretKeySpec(sha256(segredo), "AES");
    }

    public String cifrar(String cpf) {
        if (cpf == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            aleatorio.nextBytes(iv);
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] corpo = cifra.doFinal(cpf.getBytes(StandardCharsets.UTF_8));
            byte[] pacote = new byte[iv.length + corpo.length];
            System.arraycopy(iv, 0, pacote, 0, iv.length);
            System.arraycopy(corpo, 0, pacote, iv.length, corpo.length);
            return Base64.getEncoder().encodeToString(pacote);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar CPF", e);
        }
    }

    public String decifrar(String armazenado) {
        if (armazenado == null) {
            return null;
        }
        try {
            byte[] pacote = Base64.getDecoder().decode(armazenado);
            byte[] iv = new byte[IV_BYTES];
            byte[] corpo = new byte[pacote.length - IV_BYTES];
            System.arraycopy(pacote, 0, iv, 0, IV_BYTES);
            System.arraycopy(pacote, IV_BYTES, corpo, 0, corpo.length);
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cifra.doFinal(corpo), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar CPF", e);
        }
    }

    public String resumo(String cpf) {
        if (cpf == null) {
            return null;
        }
        return hex(sha256(cpf));
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder texto = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            texto.append(Character.forDigit((b >> 4) & 0xF, 16));
            texto.append(Character.forDigit(b & 0xF, 16));
        }
        return texto.toString();
    }
}
