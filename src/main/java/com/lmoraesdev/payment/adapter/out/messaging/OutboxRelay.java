package com.lmoraesdev.payment.adapter.out.messaging;

import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

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
        List<OutboxEventEntity> claimed = claimBatch();

        for (OutboxEventEntity event : claimed) {
            publish(event);
        }
    }

    @Transactional
    public List<OutboxEventEntity> claimBatch() {
        List<OutboxEventEntity> claimed = repository.findBatchForUpdateSkipLocked();
        claimed.forEach(OutboxEventEntity::markInFlight);
        return repository.saveAll(claimed);
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get();
            markPublished(event.getId());
        } catch (Exception e) {
            Logger5w1hBuilder.create(OutboxRelay.class)
                    .where("OutboxRelay")
                    .what("outbox_publish_failed")
                    .why("kafka send failed for event " + event.getId() + ", will retry next poll")
                    .who("system")
                    .how("publishPending")
                    .error(e);
            revertToPending(event.getId());
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

    @Transactional
    public void revertToPending(UUID eventId) {
        repository
                .findById(eventId)
                .ifPresent(
                        event -> {
                            event.revertToPending();
                            repository.save(event);
                        });
    }
}
