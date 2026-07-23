package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort.StoredIdempotency;
import com.lmoraesdev.payment.domain.exception.IdempotencyConflictException;
import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
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
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("CreateChargeService")
@ExtendWith(MockitoExtension.class)
class CreateChargeServiceTest {

    @Mock ChargeCreationCoordinator chargeCreationCoordinator;

    @Mock IdempotencyPort idempotencyPort;

    CreateChargeService service;

    @BeforeEach
    void setUp() {
        service = new CreateChargeService(chargeCreationCoordinator, idempotencyPort);
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

    private static CreateChargeResult aResult(String amount, boolean replayed) {
        return new CreateChargeResult(
                UUID.randomUUID(), "ACTIVE", new BigDecimal(amount), Instant.now(), replayed);
    }

    @ParameterizedTest
    @MethodSource("validAmounts")
    @DisplayName(
            "sem registro de idempotência existente, delega criação ao ChargeCreationCoordinator")
    void delegatesCreationToCoordinatorWhenNoExistingIdempotencyRecord(Case c) {
        when(idempotencyPort.findByKey("key-" + c.name())).thenReturn(Optional.empty());
        CreateChargeResult expected = aResult(c.amount(), false);
        when(chargeCreationCoordinator.createAndPersist(any(), any(), any())).thenReturn(expected);

        CreateChargeResult result =
                service.create(
                        new CreateChargeCommand(new BigDecimal(c.amount()), "key-" + c.name()));

        assertThat(result).isEqualTo(expected);
        verify(chargeCreationCoordinator).createAndPersist(any(), any(), any());
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount zero ou negativo sem tocar em nada")
    void propagatesExceptionForInvalidAmount() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateChargeCommand(BigDecimal.ZERO, "key-invalid")))
                .isInstanceOf(InvalidAmountException.class);
        verify(idempotencyPort, never()).findByKey(any());
        verify(chargeCreationCoordinator, never()).createAndPersist(any(), any(), any());
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount nulo")
    void propagatesExceptionForNullAmount() {
        assertThatThrownBy(() -> service.create(new CreateChargeCommand(null, "key-null")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("idempotency key repetida com mesmo body retorna replay sem chamar coordinator")
    void returnsReplayForRepeatedKeyWithSameBody() {
        CreateChargeResult stored = aResult("100.00", false);
        when(idempotencyPort.findByKey("key-replay"))
                .thenReturn(Optional.of(new StoredIdempotency(sha256("100.00"), stored)));

        CreateChargeResult result =
                service.create(new CreateChargeCommand(new BigDecimal("100.00"), "key-replay"));

        assertThat(result.id()).isEqualTo(stored.id());
        assertThat(result.status()).isEqualTo(stored.status());
        assertThat(result.amount()).isEqualByComparingTo(stored.amount());
        assertThat(result.createdAt()).isEqualTo(stored.createdAt());
        assertThat(result.replayed()).isTrue();
        verify(chargeCreationCoordinator, never()).createAndPersist(any(), any(), any());
    }

    @Test
    @DisplayName("idempotency key repetida com body diferente lança IdempotencyConflictException")
    void throwsConflictForRepeatedKeyWithDifferentBody() {
        CreateChargeResult stored = aResult("100.00", false);
        when(idempotencyPort.findByKey("key-conflict"))
                .thenReturn(Optional.of(new StoredIdempotency(sha256("100.00"), stored)));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateChargeCommand(
                                                new BigDecimal("200.00"), "key-conflict")))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(chargeCreationCoordinator, never()).createAndPersist(any(), any(), any());
    }

    @Test
    @DisplayName(
            "duas requisições concorrentes com a mesma key nova: violação de constraint no"
                    + " coordinator resulta em replay da vencedora, não em erro cru")
    void returnsWinnersReplayWhenCoordinatorThrowsConstraintViolation() {
        CreateChargeResult winner = aResult("100.00", false);
        when(idempotencyPort.findByKey("key-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new StoredIdempotency(sha256("100.00"), winner)));
        when(chargeCreationCoordinator.createAndPersist(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        CreateChargeResult result =
                service.create(new CreateChargeCommand(new BigDecimal("100.00"), "key-race"));

        assertThat(result.id()).isEqualTo(winner.id());
        assertThat(result.replayed()).isTrue();
        verify(idempotencyPort, org.mockito.Mockito.times(2)).findByKey("key-race");
    }

    @Test
    @DisplayName(
            "violação de constraint sem registro de idempotência encontrado depois propaga"
                    + " IllegalStateException")
    void propagatesIllegalStateExceptionWhenNoRecordFoundAfterConstraintViolation() {
        when(idempotencyPort.findByKey("key-anomaly")).thenReturn(Optional.empty());
        when(chargeCreationCoordinator.createAndPersist(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateChargeCommand(
                                                new BigDecimal("100.00"), "key-anomaly")))
                .isInstanceOf(IllegalStateException.class);
    }
}
