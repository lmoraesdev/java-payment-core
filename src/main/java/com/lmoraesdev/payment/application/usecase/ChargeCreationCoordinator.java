package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.event.ChargeCreatedEvent;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChargeCreationCoordinator {

    private final ChargeRepository chargeRepository;
    private final OutboxEventPort outboxEventPort;
    private final IdempotencyPort idempotencyPort;
    private final Counter chargesCreatedCounter;

    public ChargeCreationCoordinator(
            ChargeRepository chargeRepository,
            OutboxEventPort outboxEventPort,
            IdempotencyPort idempotencyPort,
            MeterRegistry meterRegistry) {
        this.chargeRepository = chargeRepository;
        this.outboxEventPort = outboxEventPort;
        this.idempotencyPort = idempotencyPort;
        this.chargesCreatedCounter = meterRegistry.counter("charges_created_total");
    }

    @Transactional
    public CreateChargeResult createAndPersist(
            CreateChargeCommand command, Money amount, String requestHash) {
        Charge charge = Charge.create(amount);

        Charge saved = chargeRepository.save(charge);

        outboxEventPort.record(
                "Charge",
                saved.getId().toString(),
                "ChargeCreated",
                new ChargeCreatedEvent(
                        saved.getId(), saved.getAmount().amount(), saved.getCreatedAt()));

        Logger5w1hBuilder.create(ChargeCreationCoordinator.class)
                .where("ChargeCreationCoordinator")
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
}
