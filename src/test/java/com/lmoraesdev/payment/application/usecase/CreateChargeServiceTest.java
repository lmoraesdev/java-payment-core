package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort.StoredIdempotency;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.domain.exception.IdempotencyConflictException;
import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CreateChargeService")
@ExtendWith(MockitoExtension.class)
class CreateChargeServiceTest {

    @Mock ChargeRepository chargeRepository;

    @Mock OutboxEventPort outboxEventPort;

    @Mock IdempotencyPort idempotencyPort;

    SimpleMeterRegistry meterRegistry;

    CreateChargeService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service =
                new CreateChargeService(
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

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("validAmounts")
    @DisplayName("cria cobrança nova, grava outbox e registra idempotency record")
    void createsChargeSuccessfully(Case c) {
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateChargeResult result =
                service.create(
                        new CreateChargeCommand(new BigDecimal(c.amount()), "key-" + c.name()));

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(ChargeStatus.ACTIVE.name());
        assertThat(result.amount()).isEqualByComparingTo(c.amount());
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.replayed()).isFalse();
        verify(chargeRepository).save(any());
        verify(outboxEventPort).record(eq("Charge"), any(), eq("ChargeCreated"), any());
        verify(idempotencyPort).save(eq("key-" + c.name()), any(), any(), eq(result));
        assertThat(meterRegistry.counter("charges_created_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount zero ou negativo")
    void propagatesExceptionForInvalidAmount() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateChargeCommand(BigDecimal.ZERO, "key-invalid")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount nulo")
    void propagatesExceptionForNullAmount() {
        assertThatThrownBy(() -> service.create(new CreateChargeCommand(null, "key-null")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("idempotency key repetida com mesmo body retorna replay sem criar charge nova")
    void returnsReplayForRepeatedKeyWithSameBody() {
        CreateChargeResult stored =
                new CreateChargeResult(
                        UUID.randomUUID(),
                        "ACTIVE",
                        new BigDecimal("100.00"),
                        Instant.now(),
                        false);
        when(idempotencyPort.findByKey("key-replay"))
                .thenReturn(Optional.of(new StoredIdempotency(sha256("100.00"), stored)));

        CreateChargeResult result =
                service.create(new CreateChargeCommand(new BigDecimal("100.00"), "key-replay"));

        assertThat(result.id()).isEqualTo(stored.id());
        assertThat(result.status()).isEqualTo(stored.status());
        assertThat(result.amount()).isEqualByComparingTo(stored.amount());
        assertThat(result.createdAt()).isEqualTo(stored.createdAt());
        assertThat(result.replayed()).isTrue();
        verify(chargeRepository, never()).save(any());
        verify(outboxEventPort, never()).record(any(), any(), any(), any());
        assertThat(meterRegistry.counter("charges_created_total").count()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("idempotency key repetida com body diferente lança IdempotencyConflictException")
    void throwsConflictForRepeatedKeyWithDifferentBody() {
        CreateChargeResult stored =
                new CreateChargeResult(
                        UUID.randomUUID(),
                        "ACTIVE",
                        new BigDecimal("100.00"),
                        Instant.now(),
                        false);
        when(idempotencyPort.findByKey("key-conflict"))
                .thenReturn(Optional.of(new StoredIdempotency(sha256("100.00"), stored)));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateChargeCommand(
                                                new BigDecimal("200.00"), "key-conflict")))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(chargeRepository, never()).save(any());
    }
}
