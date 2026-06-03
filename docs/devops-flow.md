# DevOps Flow — payment-api

> Fluxo de desenvolvimento, CI/CD e observabilidade do projeto.

```mermaid
flowchart LR
    subgraph DEV["💻 Dev Local"]
        SB["Spring Boot\n:8080"]
    end

    subgraph VCS["🔀 Git"]
        BR["develop / main"]
    end

    subgraph CI["⚙️ CI — GitHub Actions"]
        direction TB
        LINT["Spotless + Checkstyle"]
        UNIT["./mvnw test"]
        IT["./mvnw verify\n(Testcontainers)"]
        DOCKER["docker build"]
        LINT --> UNIT --> IT --> DOCKER
    end

    subgraph REG["📦 Registry"]
        IMG["Docker Image\nGHCR / DockerHub"]
    end

    subgraph CLOUD["☁️ Cloud Deploy"]
        direction TB
        DENV["Dev"]
        STAGE["Staging"]
        PROD["Prod"]
    end

    subgraph APP["🚀 API — Spring Boot"]
        direction TB
        CTRL["Controllers\nadapter.in.web"]
        UC["Use Cases\napplication.usecase"]
        DOM["Domain\nCharge + StateMachine"]
        CTRL --> UC --> DOM
    end

    subgraph INFRA["🗄️ Infraestrutura"]
        direction TB
        PG["PostgreSQL 18\n(JPA / Hibernate)"]
        KFK["Kafka KRaft\nChargePayedEvent"]
        RDS["Redis 7\n(idempotência — futuro)"]
    end

    subgraph OBS["🔭 Observabilidade"]
        direction TB
        PROM["Prometheus\nmétrics"]
        GRF["Grafana\ndashboard"]
        JAEG["Jaeger\ntraces OTLP"]
        LOG["Logs estruturados\n5W1H + traceId"]
        PROM --> GRF
    end

    DEV -->|push| VCS
    VCS -->|trigger| CI
    CI -->|push image| REG
    REG -->|pull & deploy| CLOUD
    CLOUD --> APP
    APP --> INFRA
    APP --> OBS
```

---

## Environments

| Env | Branch | Gatilho |
|---|---|---|
| Dev | `develop` | push → unit tests + lint |
| Staging / Prod | `main` | PR aprovado → full-verify + docker build |

## Observabilidade: Logs + Métricas + Traces

Toda requisição nasce com um **traceId** gerado pelo OpenTelemetry e propagado automaticamente:

- **Logs** — `application.yml` injeta `%X{traceId}` em cada linha via MDC
- **Métricas** — Micrometer → Prometheus → Grafana (dashboard `Payment — Overview`)
- **Traces** — Micrometer Tracing → OTLP HTTP → Jaeger (`:16686`)

> **Futuro:** Loki para log aggregation (completa o trio Logs + Métricas + Traces no Grafana).
