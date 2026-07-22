package com.lmoraesdev.payment.adapter.out.messaging;

import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "payments.charge-created";

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public OutboxRelay(
            OutboxEventJpaRepository repository, KafkaTemplate<Object, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPending() {
        List<OutboxEventEntity> pending =
                repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEventEntity event : pending) {
            publish(event);
        }
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get();
            markPublished(event.getId());
        } catch (Exception e) {
            LOG.warn("failed to publish outbox event {}, will retry next poll", event.getId(), e);
        }
    }

    @Transactional
    public void markPublished(UUID eventId) {
        repository
                .findById(eventId)
                .ifPresent(
                        event -> {
                            event.markPublished();
                            repository.save(event);
                        });
    }
}
