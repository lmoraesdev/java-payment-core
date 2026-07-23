package com.lmoraesdev.payment.application.port.out;

import com.lmoraesdev.payment.domain.model.Charge;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository {
    Charge save(Charge charge);

    Optional<Charge> findById(UUID id);

    List<Charge> findExpiredActive(Instant now);
}
