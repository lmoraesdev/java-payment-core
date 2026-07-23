package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChargeCreationCoordinator")
@ExtendWith(MockitoExtension.class)
class ChargeCreationCoordinatorTest {

    @Mock ChargeRepository chargeRepository;

    @Mock OutboxEventPort outboxEventPort;

    @Mock IdempotencyPort idempotencyPort;

    SimpleMeterRegistry meterRegistry;

    ChargeCreationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        coordinator =
                new ChargeCreationCoordinator(
                        chargeRepository, outboxEventPort, idempotencyPort, meterRegistry);
    }

    record Case(String name, String amount) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> validAmounts() {
        return Stream.of(
                new Case("centavo mínimo", "0.01"),
                new Case("valor comum", "100.00"),
                new Case("valor alto", "50000.00"));
    }

    @ParameterizedTest
    @MethodSource("validAmounts")
    @DisplayName("cria cobrança nova, grava outbox e registra idempotency record")
    void createAndPersistCreatesChargeSuccessfully(Case c) {
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateChargeResult result =
                coordinator.createAndPersist(
                        new CreateChargeCommand(new BigDecimal(c.amount()), "key-" + c.name()),
                        ChargeTestData.money(c.amount()),
                        "hash-" + c.name());

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(ChargeStatus.ACTIVE.name());
        assertThat(result.amount()).isEqualByComparingTo(c.amount());
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.replayed()).isFalse();
        verify(chargeRepository).save(any());
        verify(outboxEventPort).record(eq("Charge"), any(), eq("ChargeCreated"), any());
        verify(idempotencyPort)
                .save(eq("key-" + c.name()), eq("hash-" + c.name()), any(), eq(result));
        assertThat(meterRegistry.counter("charges_created_total").count()).isEqualTo(1.0);
    }
}
