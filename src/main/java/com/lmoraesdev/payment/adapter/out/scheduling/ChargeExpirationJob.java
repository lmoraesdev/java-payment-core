package com.lmoraesdev.payment.adapter.out.scheduling;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.event.ChargeStatusChangedEvent;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChargeExpirationJob {

    private final ChargeRepository chargeRepository;
    private final OutboxEventPort outboxEventPort;

    public ChargeExpirationJob(ChargeRepository chargeRepository, OutboxEventPort outboxEventPort) {
        this.chargeRepository = chargeRepository;
        this.outboxEventPort = outboxEventPort;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireOverdueCharges() {
        List<Charge> expired = chargeRepository.findExpiredActive(Instant.now());

        for (Charge charge : expired) {
            expire(charge);
        }
    }

    private void expire(Charge charge) {
        ChargeStatus previousStatus = charge.getStatus();

        charge.transitionTo(ChargeStatus.EXPIRED);

        chargeRepository.save(charge);

        outboxEventPort.record(
                "Charge",
                charge.getId().toString(),
                "ChargeExpired",
                new ChargeStatusChangedEvent(
                        charge.getId(),
                        previousStatus.name(),
                        ChargeStatus.EXPIRED.name(),
                        Instant.now()));

        Logger5w1hBuilder.create(ChargeExpirationJob.class)
                .where("ChargeExpirationJob")
                .what("charge_state_transitioned")
                .why("ttl_expired")
                .who("system")
                .how("scheduled_expiration")
                .info();
    }
}
