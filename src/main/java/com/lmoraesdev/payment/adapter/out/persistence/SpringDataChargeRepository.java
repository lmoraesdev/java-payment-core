package com.lmoraesdev.payment.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChargeRepository extends JpaRepository<ChargeJpaEntity, UUID> {}
