# Sistema para Gestão de Apostas Esportivas

Implementação parcial da Sprint 3 do trabalho prático de **DCC603 — Engenharia de Software** (UFMG), Grupo 8.

O código segue o que o grupo fechou nas Sprints 1 e 2: cadastro e autenticação, partidas, três tipos de aposta, probabilidade com suavização de Laplace, prêmio, apuração e relatórios. A interface principal é a web. A API HTTP usa os mesmos serviços do console. Os testes de regra continuam em memória; a execução normal grava no Postgres.

## O que está implementado

- UC01 a UC13, com as regras RN01 a RN08.
- Tipos de previsão: equipe vencedora (mandante, empate ou visitante), placar exato (0 a 9 gols por equipe) e total de cartões amarelos (0 a 10; totais acima de 10 contam como 10).
- Probabilidade `p = (n + 1) / (N + K)` e prêmio `valor × max(1,01, 0,90 / p)`, arredondado para centavos. O prêmio creditado já inclui a devolução do valor apostado.
- Até 10 apostas abertas por apostador, no máximo uma aberta por partida, só em partida `AGENDADA` que ainda não começou.
- Apuração na mesma operação que encerra a partida. Se uma gravação falha, partida, saldos, apostas e auditoria voltam ao estado anterior.
- Senha com BCrypt no Postgres. Os testes em memória continuam com SHA-256 e sal, atrás da mesma interface `SenhaHasher`. Bloqueio de 15 minutos após cinco falhas seguidas.
- CPF cifrado em repouso (AES-GCM). A busca usa o resumo SHA-256, e o domínio continua vendo o CPF em claro.
- Sessão persistida. O token deixa de valer no `DELETE /api/auth/sessao`.
- Depósito de saldo simulado, porque não há meio de pagamento no escopo.

## O que esta entrega não faz

Não há TLS: o navegador fala com o Vite em HTTP, e o proxy encaminha `/api` para a API. Não há cobrança real.

O histórico entre duas equipes conta o desfecho pelo papel naquela partida (mandante, visitante ou placar da casa), inclusive quando o mando de campo inverte. É a leitura da seção 4.3: “em 8 confrontos, o mandante venceu 4”.

## Interface Web (Frontend) e Backend

O projeto conta agora com um **Frontend completo e responsivo** construído em React, Vite e Tailwind CSS, integrado à API REST HTTP do backend Java (`ServidorHttp`).

### Como rodar o Frontend e Backend

#### Opção A: Executar via Docker Compose (Recomendado)
Para iniciar simultaneamente o backend Java (porta 8080) e o frontend web (porta 5173):

```bash
docker compose up
```

Acesse a interface no navegador: **`http://localhost:5173`**.

O compose sobe o Postgres na porta **5434**, a API na **8080** e o frontend na **5173**.

Para subir só o banco e rodar a API na máquina:

```bash
docker compose up postgres
mvn -q exec:java
```

`GIGABET_ARMAZENAMENTO=memoria` volta para a implementação em memória, sem Postgres.

#### Opção B: Diretamente na máquina

1. **Iniciar o Backend Java (API REST na porta 8080):** o Postgres da opção A precisa estar no ar.
```bash
mvn test
mvn -q exec:java
```

2. **Iniciar o Frontend Web (porta 5173):**
Em outro terminal:
```bash
cd frontend
npm install
npm run dev
```
Acesse **`http://localhost:5173`**.

Administrador inicial, criado na subida:

- e-mail: `admin@apostas.local`
- senha: `admin123`

### Sincronização ESPN

A API consulta a ESPN ao iniciar o servidor e depois a cada hora. Como o endpoint do Brasileirão rejeita intervalos
`dates=início-fim`, as consultas são feitas uma data por requisição e os eventos são consolidados por ID. O mural atualiza
as partidas a cada minuto. A carga de partidas fictícias de demonstração fica desativada por padrão; para ativá-la
explicitamente em ambiente de desenvolvimento, configure `GIGABET_SEED_DEMO=true`.

Os dados já gravados no volume do Postgres não são apagados pela sincronização nem ao atualizar a aplicação. Para zerar
uma instalação local de desenvolvimento, `docker compose down -v` remove o banco inteiro (incluindo contas e apostas),
e `docker compose up --build` cria um banco novo.

O apostador começa com saldo zero. Use a opção de depósito simulado antes de apostar. Datas no console são no fuso de São Paulo, no formato `dd/MM/yyyy HH:mm`.

## Onde está cada parte

| Pacote | Papel |
| --- | --- |
| `dominio` | `Usuario`, `Apostador`, `Administrador`, `Partida`, `Aposta` e subclasses |
| `aplicacao` | serviços, motor de cálculo (Strategy) e DTOs |
| `persistencia` | interfaces, memória (testes) e JDBC/Postgres |
| `web` | API HTTP em `/api` |
| `console` | menu de texto, no mesmo armazenamento da API |

Os testes cobrem os dois exemplos numéricos da Sprint 2 (R$ 50,00 pagam R$ 99,00; R$ 10,00 no placar 2×1 pagam R$ 486,00), os fluxos alternativos de aposta e resultado, o bloqueio de login e a reversão da apuração.
