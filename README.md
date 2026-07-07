# payment-api

Core de pagamentos Pix em Java — Hexagonal Architecture, observabilidade e pipeline CI/CD. EPIC-001 (criar cobrança) implementado e testado.

[![CI](https://github.com/lmoraesdev/java-payment-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/lmoraesdev/java-payment-hexagonal/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot&logoColor=white)

## Status

| EPIC | Descrição | Status |
|---|---|---|
| EPIC-001 | Criar cobrança — domínio, persistência, REST, tratamento de erros, testes | ✅ Concluído |
| EPIC-002 | Buscar cobrança (`GET /charges/{id}`) + idempotência | 🔲 Roadmap |
| EPIC-003 | Publicar `ChargeCreated` no Kafka | 🔲 Roadmap |
| EPIC-008 | Flyway — migrações de schema versionadas | 🔲 Roadmap |

## Stack

| Tecnologia | Versão | Para quê |
|---|---|---|
| Java | 21 LTS | Runtime |
| Spring Boot | 3.5.3 | Framework web, DI, auto-configuração |
| PostgreSQL | 18 | Persistência principal |
| Kafka (KRaft) | 3.9 | Event streaming — sem Zookeeper |
| Redis | 7 | Cache / idempotência (pré-instalado, profile `cache`) |
| SpringDoc OpenAPI | 2.8.17 | Swagger UI + spec OpenAPI 3 |
| Prometheus + Grafana | latest | Métricas + dashboard Payment Overview pré-provisionado |
| Jaeger + OpenTelemetry | latest | Distributed tracing via OTLP HTTP |
| Testcontainers | 1.21 | Testes de integração com PostgreSQL 18 real |
| Spotless (GJF AOSP) | 2.43 | Formatação automática de código |
| Checkstyle | 3.5 | Verificação de estilo |

## Arquitetura

O projeto segue Arquitetura Hexagonal (Ports & Adapters): o domínio não conhece Spring, JPA nem Kafka. Frameworks e infraestrutura ficam nas bordas; a lógica de negócio fica isolada e testável sem container.

```
com.lmoraesdev.payment
├── adapter
│   ├── in.web              ← ChargeController, GlobalExceptionHandler, DTOs
│   └── out.persistence     ← ChargeJpaEntity, ChargeMapper, ChargeRepositoryAdapter
├── application
│   ├── port.in             ← CreateCharge (interface), CreateChargeCommand, CreateChargeResult
│   ├── port.out            ← ChargeRepository (interface)
│   └── usecase             ← CreateChargeService
├── config
│   ├── logging             ← Log5w1h, Logger5w1hBuilder (structured 5W1H logging)
│   └── OpenApiConfig       ← SpringDoc / Swagger UI
└── domain
    ├── exception           ← DomainException (base), InvalidAmountException
    └── model               ← Charge, Money, ChargeStatus
```

Decisões de projeto:
- **Domínio puro** — `Charge`, `Money`, `ChargeStatus` sem nenhuma anotação de framework
- **Armazenamento monetário em centavos** — `amount_centavos BIGINT` no banco; `Money` normaliza para scale=2 no domínio; o mapper converte nos dois sentidos. Elimina risco de ponto flutuante em operações financeiras.
- **Erros tipados** — `InvalidAmountException extends DomainException` → 422; genéricos → 500
- **Logging estratégico** — só o use case loga o evento de negócio (`charge_created`); controller e adapters não logam (OTel/Jaeger cobre o fluxo)
- **Problem Details (RFC 9457)** — todos os erros retornam `ProblemDetail` com `traceId`
- **Observabilidade desde o início** — Prometheus, Grafana e Jaeger na infra antes do primeiro use case

## Como rodar

**Pré-requisitos:** Docker Desktop com WSL2 integration habilitada; contexto Docker configurado para `default`.

```bash
# 1. Variáveis de ambiente
cp .env.example .env

# 2. Subir infra + app
make up

# 3. Subir com Redis (profile cache)
docker compose --profile cache up -d --build
```

## API

### Criar cobrança

```http
POST /charges
Content-Type: application/json

{"amount": 150.00}
```

**201 Created**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "amount": 150.00,
  "createdAt": "2025-06-05T18:00:00Z"
}
```

**400 Bad Request** (amount inválido)
```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "Um ou mais campos são inválidos",
  "errors": { "amount": "must be greater than 0" },
  "traceId": "abc123..."
}
```

## Endpoints e observabilidade

| URL | O que se vê |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — documentação interativa da API |
| `http://localhost:8080/v3/api-docs` | Spec OpenAPI 3 em JSON |
| `http://localhost:8080/ping` | `{"status":"pong"}` — smoke test |
| `http://localhost:8080/actuator/health` | Status do app, banco e dependências |
| `http://localhost:8080/actuator/prometheus` | Métricas no formato Prometheus |
| `http://localhost:9090` | Prometheus — séries temporais, targets ativos |
| `http://localhost:3000` | Grafana — dashboard "Payment Overview" (credenciais do `.env`) |
| `http://localhost:16686` | Jaeger — traces distribuídos por operação |
| `http://localhost:8090` | Kafka UI — tópicos, consumer groups, mensagens |

## Testes

```bash
# Unitários — sem Docker, rápido (~2s)
./mvnw test

# Integração + cobertura — sobe PostgreSQL 18 via Testcontainers
./mvnw verify
```

| Teste | Tipo | O que cobre |
|---|---|---|
| `MoneyTest` | Unit | Validação de amount (7 casos table-driven) |
| `ChargeTest` | Unit | `create()`, `restore()`, `equals/hashCode` |
| `CreateChargeServiceTest` | Unit | Sucesso (3 valores) + erros de validação |
| `ChargeRepositoryIT` | Integration | Round-trip save/findById com PostgreSQL 18 real |
| `ChargeControllerIT` | Integration | POST 201, POST 400 Problem Details |

Convenção de nomes:
- `*Test.java` — unitários, Surefire
- `*IT.java` — integração, Failsafe + Testcontainers

Relatório JaCoCo gerado em `target/site/jacoco/index.html` após `./mvnw verify`.

## CI/CD

| Trigger | Job | O que roda |
|---|---|---|
| Push para `epic/**`, `develop` ou `main` | Lint + Unit Tests | `spotless:check` → `checkstyle:check` → `mvnw test` |
| Push para `main` ou PR → `main` | Full Verify + Docker Build | `mvnw verify` (unit + integração) → `docker build` |

## Padrões

```bash
./mvnw spotless:apply   # formata (Google Java Format, AOSP 4-space)
./mvnw spotless:check   # verifica (roda no CI)
./mvnw checkstyle:check # estilo (roda no CI)
```

**Git hooks** — shell scripts em `.githooks/` (ativar uma vez por clone):
```bash
git config core.hooksPath .githooks
```

| Hook | Ação |
|---|---|
| `commit-msg` | Valida formato Conventional Commits |
| `pre-push` | Executa `./mvnw verify` antes de subir |

Formato de commit: `tipo(escopo): descrição` — tipos: `feat fix docs style refactor test chore build ci perf revert`.

> **Nota:** os hooks são scripts shell nativos (`.githooks/`). Husky está previsto para substituí-los em versão futura.

**Makefile:**
```
make up       # docker compose up -d --build
make down     # docker compose down
make clean    # docker compose down -v (remove volumes)
make logs     # docker compose logs -f app
make test     # ./mvnw test
make verify   # ./mvnw verify
make format   # ./mvnw spotless:apply
make db       # psql no container postgres
```

---

[Leandro Moraes](https://github.com/lmoraesdev)
