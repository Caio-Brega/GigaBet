package br.ufmg.dcc603.apostas.aplicacao;

public enum CodigoRegra {
    TERMO_NAO_ACEITO("O cadastro exige o aceite do termo de tratamento de dados."),
    DADOS_INVALIDOS("Dados invalidos."),
    CPF_DUPLICADO("CPF ja cadastrado."),
    EMAIL_DUPLICADO("E-mail ja cadastrado."),
    CREDENCIAIS_INVALIDAS("E-mail ou senha invalidos."),
    CONTA_BLOQUEADA("Conta bloqueada temporariamente apos tentativas sem sucesso."),
    SESSAO_INVALIDA("Sessao invalida. Autentique-se novamente."),
    ACESSO_NEGADO("Operacao nao permitida para este perfil."),
    EQUIPES_IGUAIS("Mandante e visitante devem ser equipes diferentes."),
    DATA_NO_PASSADO("A data e hora de inicio nao podem estar no passado."),
    PARTIDA_NAO_ENCONTRADA("Partida nao encontrada."),
    PARTIDA_INDISPONIVEL("A partida nao esta aberta para apostas."),
    LIMITE_APOSTAS_ABERTAS("O apostador ja possui 10 apostas em aberto."),
    APOSTA_DUPLICADA_NA_PARTIDA("Ja existe aposta aberta deste apostador nesta partida."),
    VALOR_INVALIDO("O valor da aposta deve ser positivo, com no maximo duas casas decimais."),
    SALDO_INSUFICIENTE("Saldo insuficiente."),
    PREVISAO_INVALIDA("Previsao invalida para o tipo de aposta escolhido."),
    PARTIDA_JA_ENCERRADA("O resultado de uma partida encerrada nao pode ser alterado."),
    PARTIDA_AINDA_NAO_INICIADA("So e possivel registrar o resultado de uma partida em andamento."),
    RESULTADO_INVALIDO("Gols e cartoes amarelos nao podem ser negativos."),
    APOSTADOR_NAO_ENCONTRADO("Apostador nao encontrado.");

    private final String mensagem;

    CodigoRegra(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }
}
