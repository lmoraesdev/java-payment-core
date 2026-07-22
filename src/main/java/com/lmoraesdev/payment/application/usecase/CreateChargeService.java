package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.event.ChargeCreatedEvent;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateChargeService implements CreateCharge {
    private final ChargeRepository chargeRepository;
    private final OutboxEventPort outboxEventPort;

    public CreateChargeService(ChargeRepository chargeRepository, OutboxEventPort outboxEventPort) {
        this.chargeRepository = chargeRepository;
        this.outboxEventPort = outboxEventPort;
    }

    @Override
    @Transactional
    public CreateChargeResult create(CreateChargeCommand command) {

        Money amount = new Money(command.amount());

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

        return new CreateChargeResult(
                saved.getId(),
                saved.getStatus().name(),
                saved.getAmount().amount(),
                saved.getCreatedAt());
    }
}
