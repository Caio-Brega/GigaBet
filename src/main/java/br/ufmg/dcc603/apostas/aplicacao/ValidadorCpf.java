package br.ufmg.dcc603.apostas.aplicacao;

public final class ValidadorCpf {
    private ValidadorCpf() {
    }

    public static String normalizar(String cpf) {
        if (cpf == null) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "CPF obrigatorio.");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (!valido(digitos)) {
            throw new RegraNegocioException(CodigoRegra.DADOS_INVALIDOS, "CPF invalido.");
        }
        return digitos;
    }

    public static boolean valido(String digitos) {
        if (digitos == null || digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digito(digitos, 9) == digitos.charAt(9) - '0'
                && digito(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int digito(String digitos, int tamanho) {
        int soma = 0;
        int peso = tamanho + 1;
        for (int i = 0; i < tamanho; i++) {
            soma += (digitos.charAt(i) - '0') * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
