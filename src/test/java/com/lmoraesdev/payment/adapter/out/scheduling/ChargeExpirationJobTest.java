package com.lmoraesdev.payment.adapter.out.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@DisplayName("ChargeExpirationJob")
@ExtendWith(MockitoExtension.class)
class ChargeExpirationJobTest {

    @Mock ChargeRepository chargeRepository;

    @Mock ChargeExpirationCoordinator chargeExpirationCoordinator;

    @InjectMocks ChargeExpirationJob job;

    @Test
    @DisplayName("charge vencida é reivindicada via ChargeExpirationCoordinator")
    void expiresOverdueChargeThroughCoordinator() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        when(chargeRepository.findExpiredActive(any())).thenReturn(List.of(charge));

        job.expireOverdueCharges();

        verify(chargeExpirationCoordinator).expireOne(charge);
    }

    @Test
    @DisplayName("lista vazia não chama o coordinator")
    void doesNothingWhenListIsEmpty() {
        when(chargeRepository.findExpiredActive(any())).thenReturn(List.of());

        job.expireOverdueCharges();

        verify(chargeExpirationCoordinator, never()).expireOne(any());
    }

    @Test
    @DisplayName(
            "conflito de otimistic locking numa charge não impede as outras de serem processadas"
                    + " nem propaga")
    void skipsChargeOnOptimisticLockConflictWithoutStoppingOthers() {
        Charge conflicting = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        Charge healthy = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        when(chargeRepository.findExpiredActive(any())).thenReturn(List.of(conflicting, healthy));
        doThrow(new OptimisticLockingFailureException("stale charge"))
                .when(chargeExpirationCoordinator)
                .expireOne(conflicting);

        assertThatCode(() -> job.expireOverdueCharges()).doesNotThrowAnyException();

        verify(chargeExpirationCoordinator).expireOne(conflicting);
        verify(chargeExpirationCoordinator).expireOne(healthy);
    }
}
