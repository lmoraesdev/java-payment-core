package com.lmoraesdev.payment.adapter.out.scheduling;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.model.Charge;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChargeExpirationJob {

    private final ChargeRepository chargeRepository;
    private final ChargeExpirationCoordinator chargeExpirationCoordinator;

    public ChargeExpirationJob(
            ChargeRepository chargeRepository,
            ChargeExpirationCoordinator chargeExpirationCoordinator) {
        this.chargeRepository = chargeRepository;
        this.chargeExpirationCoordinator = chargeExpirationCoordinator;
    }

    @Scheduled(fixedDelay = 60000)
    public void expireOverdueCharges() {
        List<Charge> expired = chargeRepository.findExpiredActive(Instant.now());

        for (Charge charge : expired) {
            try {
                chargeExpirationCoordinator.expireOne(charge);
            } catch (OptimisticLockingFailureException e) {
                logSkippedDueToConflict(charge, e);
            }
        }
    }

    private void logSkippedDueToConflict(Charge charge, OptimisticLockingFailureException e) {
        Logger5w1hBuilder.create(ChargeExpirationJob.class)
                .where("ChargeExpirationJob")
                .what("charge_expiration_skipped")
                .why(
                        "optimistic lock conflict on charge "
                                + charge.getId()
                                + ", updated concorrentemente, tentará de novo no próximo poll")
                .who("system")
                .how("scheduled_expiration")
                .error(e);
    }
}
