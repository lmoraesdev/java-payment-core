# payment-api

Fundação de um core de pagamentos Pix em Java — infraestrutura, observabilidade e pipeline CI/CD prontos; domínio em construção.

[![CI](https://github.com/lmoraesdev/java-payment-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/lmoraesdev/java-payment-hexagonal/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot&logoColor=white)

## Status

A plataforma está completa: serviços sobem, métricas chegam no Grafana, traces no Jaeger, testes de integração passam com Testcontainers. O que ainda não existe é o domínio de negócio — entidades, casos de uso, portas e adapters de persistência/mensageria. Esses são os próximos passos (ver [Roadmap](#roadmap)).

## Stack

| Tecnologia | Versão | Para quê |
|---|---|---|
| Java | 21 LTS | Runtime |
| Spring Boot | 3.5.3 | Framework web, DI, auto-configuração |
| PostgreSQL | 18 | Persistência principal |
| Kafka (KRaft) | 3.9 | Event streaming — sem Zookeeper |
| Redis | 7 | Cache / idempotência (pré-instalado, profile `cache`) |
| Prometheus + Grafana | latest | Métricas + dashboard Payment Overview pré-provisionado |
| Jaeger + OpenTelemetry | latest | Distributed tracing via OTLP HTTP |
| Testcontainers | 1.21 | Testes de integração com banco real |
| Spotless (GJF AOSP) | 2.43 | Formatação automática de código |
| Checkstyle | 3.5 | Verificação de estilo |

## Arquitetura

O projeto segue Arquitetura Hexagonal (Ports & Adapters): o domínio não conhece Spring, JPA nem Kafka. Frameworks e infraestrutura ficam nas bordas; a lógica de negócio fica isolada e testável sem container.

Decisões de projeto:
- **Observabilidade desde o início** — Prometheus, Grafana e Jaeger estão na infra antes do primeiro use case existir. Métricas e traces não são afterthought.
- **Logging estruturado 5W1H** — cada log emite JSON com `where`, `why`, `when`, `who`, `what`, `how` + `traceId`/`spanId` injetados automaticamente pelo Micrometer MDC. Facilita correlação em produção.
- **Event-driven preparado** — Kafka configurado com KRaft (sem Zookeeper), consumer/producer prontos no `application.yml`. Nenhum evento publicado ainda.

```
com.lmoraesdev.payment
├── adapter
│   ├── in.web              ← PingController, GlobalExceptionHandler
│   └── out
│       ├── messaging       ← (roadmap — Kafka producers)
│       └── persistence     ← (roadmap — JPA repositories)
├── application
│   ├── port.in             ← (roadmap — interfaces de entrada)
│   ├── port.out            ← (roadmap — interfaces de saída)
│   └── usecase             ← (roadmap — casos de uso)
├── config
│   └── logging             ← Log5w1h, Logger5w1hBuilder
└── domain
    ├── event               ← (roadmap — domain events)
    ├── exception           ← DomainException (base abstrata com código de erro)
    └── model               ← (roadmap — entidades e value objects)
```

Especificação do domínio planejado (Charge Pix, máquina de estados, Money, idempotência): [`docs/architecture.md`](docs/architecture.md).

## Como rodar

**Pré-requisitos:** Docker Desktop com WSL2 integration habilitada; contexto Docker configurado para `default` (`docker context use default`).

```bash
# 1. Variáveis de ambiente
cp .env.example .env

# 2. Subir infra + app (sem Redis)
make up
# equivalente: docker compose up -d --build

# 3. Subir com Redis (profile cache)
docker compose --profile cache up -d --build

# 4. Verificar status
docker compose ps
```

## Endpoints e observabilidade

| URL | O que se vê |
|---|---|
| `http://localhost:8080/ping` | `{"status":"pong"}` — smoke test |
| `http://localhost:8080/actuator/health` | Status do app, banco e dependências |
| `http://localhost:8080/actuator/prometheus` | Métricas no formato Prometheus |
| `http://localhost:8090` | Kafka UI — tópicos, consumer groups, mensagens |
| `http://localhost:9090` | Prometheus — séries temporais, targets ativos |
| `http://localhost:3000` | Grafana — dashboard "Payment Overview" (admin/admin) |
| `http://localhost:16686` | Jaeger — traces distribuídos por operação |

## Testes

```bash
# Unitários — sem Docker, rápido
make test
# equivalente: ./mvnw test

# Integração — sobe PostgreSQL 18 via Testcontainers
make verify
# equivalente: ./mvnw verify
```

Convenção de nomes:
- `*Test.java` — testes unitários, executados pelo Surefire
- `*IT.java` — testes de integração, executados pelo Failsafe

O teste `PaymentApiApplicationIT` valida que o contexto Spring sobe corretamente contra um banco PostgreSQL real, sem mocks.

## CI/CD

| Trigger | Job | O que roda |
|---|---|---|
| Push para `develop` ou `main` | Lint + Unit Tests | `spotless:check` → `checkstyle:check` → `mvnw test` |
| Push para `main` ou PR → `main` | Full Verify + Docker Build | `mvnw verify` (unit + integração) → `docker build` |

O job de integração roda apenas no caminho para `main`, mantendo o ciclo de feedback rápido no `develop`.

## Padrões

**Formatação e estilo:**

```bash
./mvnw spotless:apply   # formata (Google Java Format, AOSP 4-space)
./mvnw spotless:check   # verifica (roda no CI)
./mvnw checkstyle:check # estilo (roda no CI)
```

**Git hooks** (ativar uma vez por clone):

```bash
git config core.hooksPath .githooks
```

| Hook | Ação |
|---|---|
| `commit-msg` | Valida formato Conventional Commits |
| `pre-push` | Executa `./mvnw verify` antes de subir |

Formato de commit: `tipo(escopo): descrição` — tipos aceitos: `feat fix docs style refactor test chore build ci perf revert`.

**Makefile:**

```
make up       # docker compose up -d --build
make down     # docker compose down (mantém volumes)
make clean    # docker compose down -v (remove volumes)
make logs     # docker compose logs -f app
make test     # ./mvnw test
make verify   # ./mvnw verify
make format   # ./mvnw spotless:apply
make db       # psql no container postgres
```

## Roadmap

O que está especificado em [`docs/architecture.md`](docs/architecture.md) e ainda não implementado:

- [ ] Entidade `Charge` com value object `Money` e `ChargeStatus`
- [ ] Máquina de estados (`PENDING → ACTIVE → PAID / EXPIRED / CANCELLED`)
- [ ] Caso de uso `CreateCharge` com idempotência por header
- [ ] Adapter de persistência JPA (`ChargeRepository`)
- [ ] Publicação de eventos de domínio no Kafka (`ChargeCreated`, `ChargePaid`, etc.)
- [ ] Consumer para eventos externos (webhook/notificação)
- [ ] Uso do Redis para cache de idempotência e locks
- [ ] Endpoints REST de cobrança (`POST /charges`, `GET /charges/{id}`)

---

[Leandro Moraes](https://github.com/lmoraesdev)
