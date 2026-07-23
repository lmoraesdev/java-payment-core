package com.lmoraesdev.payment.adapter.out.messaging;

import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxClaimCoordinator {

    private static final Duration STUCK_IN_FLIGHT_THRESHOLD = Duration.ofMinutes(2);

    private final OutboxEventJpaRepository repository;

    public OutboxClaimCoordinator(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<OutboxEventEntity> claimBatch() {
        repository.reapStuckInFlight(Instant.now().minus(STUCK_IN_FLIGHT_THRESHOLD));

        List<OutboxEventEntity> claimed = repository.findBatchForUpdateSkipLocked();
        claimed.forEach(OutboxEventEntity::markInFlight);
        return repository.saveAll(claimed);
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
