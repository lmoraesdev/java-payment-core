package com.lmoraesdev.payment.application.port.out;

import java.util.UUID;

public interface WebhookEventPort {
    boolean existsByEventId(String eventId);

    void save(String eventId, UUID chargeId);
}
