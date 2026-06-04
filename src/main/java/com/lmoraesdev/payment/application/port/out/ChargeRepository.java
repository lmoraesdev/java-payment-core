package com.lmoraesdev.payment.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.lmoraesdev.payment.domain.model.Charge;

public interface ChargeRepository {
    Charge save(Charge charge);
    Optional<Charge> findById(UUID id);
}
