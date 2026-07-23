package com.lmoraesdev.payment.adapter.out.persistence.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository
        extends JpaRepository<IdempotencyRecordEntity, UUID> {

    Optional<IdempotencyRecordEntity> findByKey(String key);
}
