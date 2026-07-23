package com.lmoraesdev.payment.adapter.out.persistence;

import com.lmoraesdev.payment.domain.model.ChargeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChargeRepository extends JpaRepository<ChargeJpaEntity, UUID> {

    List<ChargeJpaEntity> findTop50ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            ChargeStatus status, Instant instant);
}
