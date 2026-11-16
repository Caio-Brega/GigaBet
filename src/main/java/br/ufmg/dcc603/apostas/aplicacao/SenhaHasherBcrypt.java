package br.ufmg.dcc603.apostas.aplicacao;

import org.mindrot.jbcrypt.BCrypt;

public final class SenhaHasherBcrypt implements SenhaHasher {
    @Override
    public String gerar(String senha) {
        return BCrypt.hashpw(senha, BCrypt.gensalt(10));
    }

    @Override
    public boolean confere(String senha, String hashArmazenado) {
        if (senha == null || hashArmazenado == null || !hashArmazenado.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(senha, hashArmazenado);
    }
}
