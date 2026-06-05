package com.lmoraesdev.payment.application.port.out;

import com.lmoraesdev.payment.domain.model.Charge;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository {
    Charge save(Charge charge);

    Optional<Charge> findById(UUID id);
}
