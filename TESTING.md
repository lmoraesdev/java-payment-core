# Testing Standard (Java) — portado do Test Table Pattern

Este é o padrão de testes do projeto, adaptado do meu padrão antigo de Node/TS
para os idiomas nativos do Java. Regra de ouro: **manter os princípios, NÃO
recriar a camada de helpers** — JUnit 5 + AssertJ + Mockito já entregam isso.

## Princípios que vêm do padrão antigo (valem 100%)

- Table-driven: casos de teste como dados, não como `it()` duplicados.
- Builders pra entidades de domínio; fixtures pra payloads complexos.
- Separar claramente "Success" e "Validation errors".
- Mapear TODAS as branches antes de escrever (if/else/try/for/switch e combinações).
- Testar o comportamento REAL do código, não o assumido.
- Testar resiliência em observer/orchestrator (falha parcial não derruba o fluxo).
- Refatorar use case complexo em métodos privados de responsabilidade única.
- Cobertura: use cases críticos ~100% (sucesso + erros esperados).

## Mapa Node/TS → Java (o que muda)

| Padrão antigo (vitest+sinon)        | Equivalente NATIVO em Java                          | Veredito |
| ----------------------------------- | --------------------------------------------------- | -------- |
| `runTests(testCases, cb)`           | `@ParameterizedTest` + `@MethodSource`              | usar nativo, não recriar |
| `testCases: TestCase[]`             | `Stream<TestCase>` (record) via `@MethodSource`     | porta fiel e idiomática |
| `it.skip` (testType)                | `@Disabled("motivo")`                               | nativo |
| `it.only`                           | rodar 1 teste (IDE / `-Dtest=Classe#metodo`)        | não existe data-driven; ok |
| `getStub(sandbox, X.prototype, m)`  | `@Mock` + `when(mock.m()).thenReturn/thenThrow`     | usar nativo |
| `executeStubAssertions(callMatchs)` | `verify(mock).m(args)`, `times(n)`, `ArgumentCaptor`| usar nativo |
| `getError(cb)`                      | `assertThatThrownBy(() -> ...)` / `catchThrowable`  | usar nativo |
| DI container `container.get(...)`   | unit: `new Service(mock)`; integração: `@SpringBootTest` | ver nota |
| builders `.getData()` / `.build()`  | builder retorna domínio; persistir = via repository | manter |
| fixtures `*-fixture.ts`             | `*Fixtures` (factory estática) ou builder           | manter |
| cobertura (vitest)                  | JaCoCo (`jacoco-maven-plugin`)                      | trocar ferramenta |

## O que NÃO portar (e por quê)

- **O DSL declarativo de stubs** (`input.stubs.x.resolves` + `output.stubs.callMatchs`).
  Em Java, parametrize os INPUTS e os EXPECTED; o setup de stub e o `verify` ficam
  imperativos no corpo do teste, com Mockito. Tentar codar stub-como-dado vira um
  mini-framework que briga com a linguagem.
- **Custom runner** (`runTests`). `@ParameterizedTest` é o runner.
- **DI container no unit test.** Em Java, teste UNITÁRIO instancia direto
  (`new CreateChargeService(mockRepo)`) — mais rápido e isolado, sem subir Spring.
  Container só em teste de INTEGRAÇÃO (`@SpringBootTest`/`@DataJpaTest`).

## Onde o Java é ATÉ melhor

Seu padrão antigo dizia "stubar classes concretas, não interfaces" (limitação do
sinon). No Java/Mockito você mocka a **interface** (a porta `ChargeRepository`)
diretamente — que é a inversão de dependência feita certo. Mais limpo, e alinhado
com a clean arch.

## Stack

JUnit 5 (Jupiter) · AssertJ · Mockito · Testcontainers · JaCoCo (cobertura).
Tudo via `spring-boot-starter-test`, menos o JaCoCo (plugin no pom).

---

## Exemplo 1 — Table-driven (o "testCases" do jeito Java)

```java
@DisplayName("Money")
class MoneyTest {

    record Case(String name, String amount, boolean valid) {
        @Override public String toString() { return name; }
    }

    static Stream<Case> cases() {
        return Stream.of(
            new Case("aceita centavo mínimo", "0.01", true),
            new Case("aceita valor comum",    "10.50", true),
            new Case("rejeita negativo",      "-1.00", false),
            new Case("rejeita zero",          "0.00", false)
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void validates(Case c) {
        if (c.valid()) {
            assertThat(new Money(new BigDecimal(c.amount())).amount())
                .isEqualByComparingTo(c.amount());
        } else {
            assertThatThrownBy(() -> new Money(new BigDecimal(c.amount())))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

## Exemplo 2 — Test Data Builder (BASE + sobrescreve só o que muda)

```java
public final class ChargeTestData {
    private Money amount = new Money(new BigDecimal("10.50"));
    private ChargeStatus status = ChargeStatus.ACTIVE;

    public static ChargeTestData aCharge() { return new ChargeTestData(); }
    public static Money money(String v) { return new Money(new BigDecimal(v)); }

    public ChargeTestData withAmount(String v) {
        this.amount = new Money(new BigDecimal(v));
        return this;
    }
    public ChargeTestData withStatus(ChargeStatus s) {
        this.status = s;
        return this;
    }
    public Charge build() {
        return Charge.restore(UUID.randomUUID(), amount, status, Instant.now());
    }
}
// uso: ChargeTestData.aCharge().withAmount("99.90").build();
```

## Exemplo 3 — Use case com mocks (sucesso + verificação)

```java
@ExtendWith(MockitoExtension.class)
class CreateChargeServiceTest {

    @Mock ChargeRepository repository;
    @InjectMocks CreateChargeService service;

    @Nested @DisplayName("Success")
    class Success {
        @Test
        void cria_e_persiste_a_cobranca() {
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.create(new CreateChargeCommand(new BigDecimal("10.50")));

            var captor = ArgumentCaptor.forClass(Charge.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ChargeStatus.ACTIVE);
            assertThat(result.amount()).isEqualByComparingTo("10.50");
        }
    }

    @Nested @DisplayName("Validation errors")
    class ValidationErrors {
        @Test
        void nao_persiste_quando_valor_invalido() {
            assertThatThrownBy(() ->
                service.create(new CreateChargeCommand(new BigDecimal("-1"))))
                .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }
    }
}
```

## Exemplo 4 — Resiliência de orchestrator (quando existir)

```java
when(subscriberA.notify(any())).thenReturn(...);
doThrow(new RuntimeException("falhou")).when(subscriberB).notify(any());
// act
service.execute(event);
// assert: B falhou MAS C ainda foi chamado
verify(subscriberC).notify(any());
```

## Convenções

- `*Test` → unitário (surefire, fase `test`); `*IT` → integração (failsafe, fase `verify`).
- `@Nested` pra agrupar Success / ValidationErrors. `@DisplayName` em tudo.
- Dados de borda = fixos e determinísticos. `datafaker` só pra preenchimento "qualquer válido".
- BigDecimal: sempre `isEqualByComparingTo`, nunca `isEqualTo`.

## Checklist (adaptado do antigo)

- [ ] Todas as branches cobertas? (mapeie antes de escrever)
- [ ] Sucesso e erro testados? Early returns cobertos?
- [ ] Mockou a INTERFACE (porta), não a implementação concreta?
- [ ] Unit test sem Spring (new + mock); integração com @SpringBootTest/Testcontainers?
- [ ] Verificou chamadas dos mocks (verify/ArgumentCaptor)?
- [ ] Teste reflete o comportamento REAL (não o assumido)?
- [ ] Sem helper de infra caseiro reinventando JUnit/Mockito/AssertJ?
