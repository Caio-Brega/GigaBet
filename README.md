# Sistema para Gestão de Apostas Esportivas

Implementação parcial da Sprint 3 do trabalho prático de **DCC603 — Engenharia de Software** (UFMG), Grupo 8.

O código segue o que o grupo fechou nas Sprints 1 e 2: cadastro e autenticação, partidas, três tipos de aposta, probabilidade com suavização de Laplace, prêmio, apuração e relatórios. A interface desta entrega é um console. Os dados ficam em memória, atrás das interfaces de repositório, para os testes não dependerem de banco.

## O que está implementado

- UC01 a UC13, com as regras RN01 a RN08.
- Tipos de previsão: equipe vencedora (mandante, empate ou visitante), placar exato (0 a 9 gols por equipe) e total de cartões amarelos (0 a 10; totais acima de 10 contam como 10).
- Probabilidade `p = (n + 1) / (N + K)` e prêmio `valor × max(1,01, 0,90 / p)`, arredondado para centavos. O prêmio creditado já inclui a devolução do valor apostado.
- Até 10 apostas abertas por apostador, no máximo uma aberta por partida, só em partida `AGENDADA` que ainda não começou.
- Apuração na mesma operação que encerra a partida. Se uma gravação falha, partida, saldos, apostas e auditoria voltam ao estado anterior.
- Senha armazenada com sal e SHA-256. Bloqueio de 15 minutos após cinco falhas seguidas.
- Depósito de saldo simulado, porque a integração com meios de pagamento ficou fora do escopo.

## O que esta entrega não faz

Não há interface web, banco relacional, TLS, criptografia de CPF em repouso nem BCrypt. A Sprint 2 deixa essas trocas para trás das mesmas interfaces (`SenhaHasher` e repositórios).

O histórico entre duas equipes conta o desfecho pelo papel naquela partida (mandante, visitante ou placar da casa), inclusive quando o mando de campo inverte. É a leitura da seção 4.3: “em 8 confrontos, o mandante venceu 4”.

## Como rodar

Requisito: JDK 17 ou superior e Maven.

```bash
mvn test
mvn -q exec:java
```

`mvn test` executa a suíte JUnit 5. `mvn -q exec:java` abre o console.

Administrador inicial, criado na subida:

- e-mail: `admin@apostas.local`
- senha: `admin123`

O apostador começa com saldo zero. Use a opção de depósito simulado antes de apostar. Datas no console são no fuso de São Paulo, no formato `dd/MM/yyyy HH:mm`.

## Onde está cada parte

| Pacote | Papel |
| --- | --- |
| `dominio` | `Usuario`, `Apostador`, `Administrador`, `Partida`, `Aposta` e subclasses |
| `aplicacao` | serviços, motor de cálculo (Strategy) e DTOs |
| `persistencia` | interfaces de repositório e implementação em memória, com transação |
| `console` | menu da implementação parcial |

Os testes cobrem os dois exemplos numéricos da Sprint 2 (R$ 50,00 pagam R$ 99,00; R$ 10,00 no placar 2×1 pagam R$ 486,00), os fluxos alternativos de aposta e resultado, o bloqueio de login e a reversão da apuração.
