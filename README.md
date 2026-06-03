# Payment API

API de pagamentos Pix em Java + Spring Boot com Clean Architecture, Kafka (KRaft), Postgres, Prometheus, Grafana e Jaeger.

**Documentação de arquitetura:**
- [docs/architecture.md](docs/architecture.md) — domínio, fluxos, API, pacotes, idempotência
- [docs/devops-flow.md](docs/devops-flow.md) — pipeline CI/CD + observabilidade (Mermaid)

## Stack

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 LTS |
| Framework | Spring Boot 3.5.3 |
| Messaging | Kafka 3.9 (KRaft — sem Zookeeper) |
| Persistence | PostgreSQL 18 |
| Métricas | Actuator + Micrometer + Prometheus + Grafana |
| Tracing | Micrometer Tracing + OpenTelemetry → Jaeger |
| Cache | Redis 7 (pré-instalado, profile `cache`) |
| Testes | JUnit 5 + Testcontainers |
| Qualidade | Spotless (GJF AOSP) + Checkstyle |

## Quickstart

```bash
# 1. Variáveis de ambiente
cp .env.example .env

# 2. Subir toda a infra + app
make up

# 3. Status
docker compose ps
```

| URL | Descrição |
|---|---|
| `GET /ping` | Smoke test |
| `GET /actuator/health` | Health probe |
| `GET /actuator/prometheus` | Métricas Prometheus |
| http://localhost:8090 | Kafka UI |
| http://localhost:9090 | Prometheus |
| http://localhost:3000 | Grafana — Payment Overview |
| http://localhost:16686 | Jaeger — traces |

## Subir Redis (opcional)

Redis está pré-instalado mas **não sobe por padrão**:

```bash
docker compose --profile cache up -d
```

## Makefile

```
make up       # docker compose up -d --build
make down     # docker compose down (mantém volumes)
make clean    # docker compose down -v (apaga volumes)
make logs     # docker compose logs -f app
make test     # ./mvnw test (sem Docker)
make verify   # ./mvnw verify (com Testcontainers — requer Docker)
make format   # ./mvnw spotless:apply
make db       # psql no container postgres
```

## Qualidade de código

```bash
# Formatar
./mvnw spotless:apply         # ou: make format

# Verificar formatação (GJF AOSP, 4-space)
./mvnw spotless:check

# Verificar estilo (Checkstyle)
./mvnw checkstyle:check

# CI roda os dois antes dos testes:
./mvnw spotless:check && ./mvnw checkstyle:check && ./mvnw test
```

## Pacotes (Clean Architecture)

```
com.lmoraesdev.payment
├── adapter
│   ├── in.web          ← controllers, exception handler
│   └── out
│       ├── messaging   ← Kafka producers (futuro)
│       └── persistence ← JPA repositories (futuro)
├── application
│   ├── port.in         ← interfaces de entrada (futuro)
│   ├── port.out        ← interfaces de saída (futuro)
│   └── usecase         ← casos de uso (futuro)
├── config
│   └── logging         ← Logger5w1h estruturado
└── domain
    ├── event           ← domain events (futuro)
    ├── exception       ← DomainException base
    └── model           ← entidades / value objects (futuro)
```

## Testes

```bash
# Unitários (sem Docker)
./mvnw test

# Integração — sobe Postgres 18 via Testcontainers
./mvnw verify
```

## Git hooks

```bash
git config core.hooksPath .githooks
```

| Hook | O que faz |
|---|---|
| `pre-push` | Executa `./mvnw verify` antes de cada push |
| `commit-msg` | Valida formato Conventional Commits |

Formato de commit: `tipo(escopo): descrição`
Tipos: `feat fix docs style refactor test chore build ci perf revert`

## CI

| Branch / PR | Jobs |
|---|---|
| `develop` push | Lint (Spotless + Checkstyle) + unit tests |
| `main` push / PR → main | Lint + unit tests + full-verify + docker build |
