package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort.StoredIdempotency;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.event.ChargeCreatedEvent;
import com.lmoraesdev.payment.domain.exception.IdempotencyConflictException;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateChargeService implements CreateCharge {
    private final ChargeRepository chargeRepository;
    private final OutboxEventPort outboxEventPort;
    private final IdempotencyPort idempotencyPort;
    private final Counter chargesCreatedCounter;

    public CreateChargeService(
            ChargeRepository chargeRepository,
            OutboxEventPort outboxEventPort,
            IdempotencyPort idempotencyPort,
            MeterRegistry meterRegistry) {
        this.chargeRepository = chargeRepository;
        this.outboxEventPort = outboxEventPort;
        this.idempotencyPort = idempotencyPort;
        this.chargesCreatedCounter = meterRegistry.counter("charges_created_total");
    }

    @Override
    @Transactional
    public CreateChargeResult create(CreateChargeCommand command) {

        Money amount = new Money(command.amount());
        String requestHash = hash(command.amount());

        Optional<StoredIdempotency> existing = idempotencyPort.findByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash, command.idempotencyKey());
        }

        Charge charge = Charge.create(amount);

        Charge saved = chargeRepository.save(charge);

        outboxEventPort.record(
                "Charge",
                saved.getId().toString(),
                "ChargeCreated",
                new ChargeCreatedEvent(
                        saved.getId(), saved.getAmount().amount(), saved.getCreatedAt()));

        Logger5w1hBuilder.create(CreateChargeService.class)
                .where("CreateChargeService")
                .what("charge_created")
                .why("charge creation requested")
                .who("system")
                .how("createCharge")
                .info();

        CreateChargeResult result =
                new CreateChargeResult(
                        saved.getId(),
                        saved.getStatus().name(),
                        saved.getAmount().amount(),
                        saved.getCreatedAt(),
                        false);

        idempotencyPort.save(command.idempotencyKey(), requestHash, saved.getId(), result);
        chargesCreatedCounter.increment();

        return result;
    }

    private CreateChargeResult replay(StoredIdempotency existing, String requestHash, String key) {
        if (!existing.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(key);
        }

        CreateChargeResult previous = existing.result();
        return new CreateChargeResult(
                previous.id(), previous.status(), previous.amount(), previous.createdAt(), true);
    }

    private String hash(BigDecimal amount) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(amount.toPlainString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
