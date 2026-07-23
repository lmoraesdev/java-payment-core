package com.lmoraesdev.payment.adapter.out.persistence.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value =
                    """
                    select * from outbox_events
                    where status = 'PENDING'
                    order by created_at asc
                    limit 50
                    for update skip locked
                    """,
            nativeQuery = true)
    List<OutboxEventEntity> findBatchForUpdateSkipLocked();

    @Modifying
    @Query(
            value =
                    """
                    update outbox_events
                    set status = 'PENDING', claimed_at = null
                    where status = 'IN_FLIGHT' and claimed_at < :cutoff
                    """,
            nativeQuery = true)
    int reapStuckInFlight(@Param("cutoff") Instant cutoff);
}
