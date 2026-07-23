package com.lmoraesdev.payment.adapter.out.persistence.webhook;

import com.lmoraesdev.payment.application.port.out.WebhookEventPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventPersistenceAdapter implements WebhookEventPort {

    private final WebhookEventJpaRepository repository;

    public WebhookEventPersistenceAdapter(WebhookEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public void save(String eventId, UUID chargeId) {
        repository.save(WebhookEventEntity.create(eventId, chargeId));
    }
}
