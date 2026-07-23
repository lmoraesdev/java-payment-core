# payment-api

Core de pagamentos Pix em Java, com Arquitetura Hexagonal. O foco não é só criar e consultar
cobranças — é fazer isso com garantias reais de concorrência e consistência (idempotência,
outbox transacional, optimistic locking) e observabilidade de verdade (logs estruturados 5W1H,
correlation_id de ponta a ponta, métricas e tracing distribuído). Não é "mais um CRUD de
pagamento".

[![CI](https://github.com/lmoraesdev/java-payment-core/actions/workflows/ci.yml/badge.svg)](https://github.com/lmoraesdev/java-payment-core/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot&logoColor=white)

## Stack

| Tecnologia | Versão | Para quê |
|---|---|---|
| Java | 21 LTS | Runtime |
| Spring Boot | 3.5.3 | Framework web, DI, auto-configuração |
| PostgreSQL | 18 | Persistência principal |
| Kafka (KRaft) | 3.9 | Event streaming — sem Zookeeper |
| Flyway | (via Spring Boot) | Migrações de schema versionadas (`V1`–`V5`) |
| Redis | 7 | Pré-instalado (profile `cache`), sem uso ainda |
| Micrometer + Prometheus | — | Métricas de negócio e técnicas |
| Micrometer Tracing + OpenTelemetry (OTLP) | — | Tracing distribuído, exportado pro Jaeger |
| ArchUnit | 1.3.0 | Trava em teste que o domínio não depende de framework |
| Testcontainers | (Spring Boot BOM) | Integration tests com PostgreSQL 18 real via Docker |
| JaCoCo | 0.8.13 | Cobertura de testes |
| Spotless (GJF AOSP) | 2.43 | Formatação automática de código |
| Checkstyle | 3.5 | Verificação de estilo |
| SpringDoc OpenAPI | 2.8.17 | Swagger UI + spec OpenAPI 3 |

## Arquitetura

Ports & Adapters: o domínio (`domain/**`) não conhece Spring, JPA nem qualquer outro framework.
Um teste ArchUnit ([`DomainPurityArchTest`](src/test/java/com/lmoraesdev/payment/architecture/DomainPurityArchTest.java))
garante isso em CI — quebra o build se alguma classe em `domain/**` importar
`org.springframework..` ou `jakarta.persistence..`.

```
com.lmoraesdev.payment
├── adapter
│   ├── in.web              ← controllers REST, DTOs, GlobalExceptionHandler, docs OpenAPI
│   ├── out.messaging       ← OutboxRelay (publica no Kafka) e OutboxClaimCoordinator (transacional)
│   ├── out.persistence     ← entidades JPA e adapters de Charge, outbox, idempotência e webhook
│   └── out.scheduling      ← ChargeExpirationJob e ChargeExpirationCoordinator
├── application
│   ├── port.in             ← casos de uso (interfaces) + commands/results
│   ├── port.out            ← portas de saída (repositórios, outbox, idempotência)
│   └── usecase             ← implementação dos casos de uso
├── config
│   ├── logging             ← Log5w1h, Logger5w1hBuilder (logging estruturado 5W1H)
│   └── OpenApiConfig       ← SpringDoc / Swagger UI
└── domain
    ├── event               ← eventos de domínio (payload do outbox)
    ├── exception           ← DomainException e subclasses, mapeadas a status HTTP
    └── model               ← Charge, Money, ChargeStatus — zero dependência de framework
```

## Padrões implementados

**Transactional Outbox com `SELECT ... FOR UPDATE SKIP LOCKED`.** Gravar a `Charge` e o evento de
domínio na mesma transação evita dual-write (perder o evento se o Kafka cair depois do commit).
`SKIP LOCKED` em `OutboxEventJpaRepository.findBatchForUpdateSkipLocked()` permite rodar múltiplas
instâncias do relay em paralelo sem duas instâncias publicarem o mesmo evento. As operações de
claim/publish/revert vivem em `OutboxClaimCoordinator`, um bean separado de `OutboxRelay` — se
estivessem na mesma classe chamando `this.metodo()`, o proxy do Spring nunca seria interceptado e o
`@Transactional` viraria no-op silencioso. Eventos que ficam presos em `IN_FLIGHT` por mais de 2
minutos (worker morreu no meio do processamento) são reclamados de volta pra `PENDING` antes do
próximo lote.

**Idempotency-Key com replay seguro sob concorrência.** `POST /charges` exige o header
`Idempotency-Key`; uma chave repetida com o mesmo corpo retorna a cobrança já criada (200) em vez
de duplicar. O caso interessante é a race: duas requisições com a mesma chave *nova* chegam ao
mesmo tempo, nenhuma encontra registro existente, e uma delas comita primeiro. A perdedora esbarra
na constraint única de `idempotency_records` — no Postgres, isso aborta a transação inteira, então
não dá pra simplesmente capturar a exceção e continuar consultando nela. `CreateChargeService`
captura essa violação **fora** da transação de escrita (que já rodou em `ChargeCreationCoordinator`
e sofreu rollback completo) e só então busca o registro da vencedora numa transação nova,
devolvendo o replay dela em vez de um 500 cru. Provado com um teste de concorrência real
(duas threads, mesma chave, Postgres via Testcontainers — não dá pra provar isso com mocks).

**Optimistic locking (`@Version`) no ciclo de vida da `Charge`.** Um webhook do provedor e o job de
expiração agendado podem tentar transicionar a mesma `Charge` ao mesmo tempo. `ChargeJpaEntity`
implementa `Persistable<UUID>` com `isNew() = version == null` — como o id é um UUID gerado em
código (não pelo banco), o Spring Data não teria como distinguir insert de update só pelo id, e
sem isso todo `save()` de uma charge nova viraria um `merge()` que falha com um optimistic lock
falso. Num conflito real, o job de expiração pula aquela charge nesse ciclo sem travar as outras
(cada uma processada em sua própria transação, via `ChargeExpirationCoordinator`); o caminho do
webhook deixa a exceção propagar até um handler dedicado que responde 409, para o provedor tentar
de novo.

**Deduplicação de webhook por `event_id`.** Constraint única em `webhook_events.event_id`;
`ProcessWebhookService` verifica isso antes de tocar em qualquer coisa, então reenvios do provedor
(comuns em integrações de pagamento) são no-ops seguros, não erros.

**Logging estruturado 5W1H com `correlation_id` propagado via MDC.** Em vez de mensagens de log
livres, todo evento relevante passa por `Logger5w1hBuilder` (`where`/`why`/`who`/`what`/`how`). O
`correlation_id` da requisição original é persistido junto com o evento do outbox e restaurado no
MDC quando o relay loga uma falha de publicação — mesmo atravessando o boundary assíncrono entre
"criar a cobrança" e "publicar no Kafka minutos depois", a falha continua rastreável até a
requisição que a originou.

## Endpoints

| Método | Path | Request | Response |
|---|---|---|---|
| `POST` | `/charges` | Header `Idempotency-Key` + `{"amount": 150.00}` | `201` (ou `200` em replay) com `{id, status, amount, createdAt}` |
| `GET` | `/charges/{id}` | — | `200` com `{id, status, amount, createdAt}`, `404` se não existir |
| `POST` | `/webhooks/provider` | `{"eventId", "chargeId", "status"}` (`status` ∈ `PAID`\|`EXPIRED`\|`CANCELLED`) | `200` sempre que processado (inclusive reenvio duplicado), `404`/`422`/`409` conforme o caso |

Erros seguem Problem Details (RFC 9457) — `ProblemDetail` com `traceId` no corpo.

## Como rodar

**Pré-requisito:** Docker com suporte a Compose.

```bash
cp .env.example .env
docker compose up -d --build      # ou: make up

# com Redis (profile cache, ainda sem uso pela aplicação):
docker compose --profile cache up -d --build
```

Alternativa sem Docker para o app (com Postgres e Kafka já rodando localmente nas portas padrão
`5432`/`9092`, usuário/banco `admin`/`payment_db` batendo com `.env.example`):

```bash
./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test    # unitários — sem Docker, ~poucos segundos
./mvnw verify  # unitários + integração — sobe PostgreSQL 18 real via Testcontainers
```

Convenção: `*Test.java` roda no Surefire (unitário, mocka os ports); `*IT.java` roda no Failsafe
com Testcontainers (integração, banco real). Alguns exemplos do que cada tipo cobre:

| Teste | O que prova |
|---|---|
| `CreateChargeServiceConcurrencyIT` | Duas requisições concorrentes com a mesma Idempotency-Key nova geram uma única charge; a perdedora recebe replay, não 500 |
| `OutboxClaimCoordinatorTransactionalIT` | Uma exceção no meio do claim do outbox desfaz a mudança de status (prova que o `@Transactional` funciona de verdade, não é self-invocation) |
| `ChargeExpirationJobTest` / `ChargeExpirationCoordinatorTest` | Conflito de optimistic locking numa charge não impede as outras de expirarem no mesmo ciclo |
| `ProcessWebhookServiceTest` | Status inválido é rejeitado antes de qualquer transição; conflito de lock propaga em vez de ser engolido |
| `ChargeRepositoryIT` | Round-trip save/findById preserva todos os campos, inclusive `version` |
| `DomainPurityArchTest` | `domain/**` livre de imports de Spring/JPA |

Relatório de cobertura JaCoCo em `target/site/jacoco/index.html` após `./mvnw verify`.

## Observabilidade e Swagger (local)

| URL | O que se vê |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — documentação interativa da API |
| `http://localhost:8080/v3/api-docs` | Spec OpenAPI 3 em JSON |
| `http://localhost:8080/actuator/health` | Status do app, banco e dependências |
| `http://localhost:8080/actuator/prometheus` | Métricas no formato Prometheus |
| `http://localhost:9090` | Prometheus |
| `http://localhost:3000` | Grafana — dashboard "Payment Overview" (credenciais do `.env`) |
| `http://localhost:16686` | Jaeger — traces distribuídos por operação |
| `http://localhost:8090` | Kafka UI — tópicos, consumer groups, mensagens |

## Qualidade e commits

```bash
./mvnw spotless:apply    # formata (Google Java Format, AOSP 4-space)
./mvnw spotless:check    # verifica (roda no CI)
./mvnw checkstyle:check  # estilo (roda no CI)
```

Git hooks nativos em `.githooks/` (ativar uma vez por clone com
`git config core.hooksPath .githooks`): `commit-msg` valida Conventional Commits
(`tipo(escopo): descrição`), `pre-push` roda `./mvnw verify` antes de subir.

---

[Leandro Moraes](https://github.com/lmoraesdev)
