package com.lmoraesdev.payment.application.port.out;

import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyPort {
    Optional<StoredIdempotency> findByKey(String key);

    void save(String key, String requestHash, UUID chargeId, CreateChargeResult result);

    record StoredIdempotency(String requestHash, CreateChargeResult result) {}
}
