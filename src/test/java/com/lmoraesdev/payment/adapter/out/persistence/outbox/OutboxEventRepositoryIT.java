package com.lmoraesdev.payment.adapter.out.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmoraesdev.payment.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("OutboxEventJpaRepository")
class OutboxEventRepositoryIT extends AbstractIntegrationTest {

    @Autowired OutboxEventJpaRepository repository;

    @Test
    @DisplayName("findBatchForUpdateSkipLocked retorna só PENDING, ordenado por created_at")
    void findBatchForUpdateSkipLockedReturnsOnlyPendingOrderedByCreatedAt() {
        OutboxEventEntity first = repository.save(pending("aggregate-1"));
        OutboxEventEntity second = repository.save(pending("aggregate-2"));
        OutboxEventEntity published = repository.save(pending("aggregate-3"));
        published.markPublished();
        repository.save(published);

        List<OutboxEventEntity> batch = repository.findBatchForUpdateSkipLocked();

        assertThat(batch)
                .extracting(OutboxEventEntity::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(batch)
                .extracting(OutboxEventEntity::getStatus)
                .containsOnly(OutboxStatus.PENDING);
    }

    private OutboxEventEntity pending(String aggregateId) {
        return OutboxEventEntity.pending("Charge", aggregateId, "ChargeCreated", "{}");
    }
}
