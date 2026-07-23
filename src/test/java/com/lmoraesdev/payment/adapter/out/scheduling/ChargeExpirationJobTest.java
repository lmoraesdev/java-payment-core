package com.lmoraesdev.payment.adapter.out.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
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

@DisplayName("ChargeExpirationJob")
@ExtendWith(MockitoExtension.class)
class ChargeExpirationJobTest {

    @Mock ChargeRepository chargeRepository;

    @Mock OutboxEventPort outboxEventPort;

    @InjectMocks ChargeExpirationJob job;

    @Test
    @DisplayName("charge vencida transiciona pra EXPIRED, salva e grava outbox")
    void expiresOverdueCharge() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        when(chargeRepository.findExpiredActive(any())).thenReturn(List.of(charge));

        job.expireOverdueCharges();

        assertThat(charge.getStatus()).isEqualTo(ChargeStatus.EXPIRED);
        verify(chargeRepository).save(charge);
        verify(outboxEventPort)
                .record(eq("Charge"), eq(charge.getId().toString()), eq("ChargeExpired"), any());
    }

    @Test
    @DisplayName("lista vazia não faz nada")
    void doesNothingWhenListIsEmpty() {
        when(chargeRepository.findExpiredActive(any())).thenReturn(List.of());

        job.expireOverdueCharges();

        verify(chargeRepository, never()).save(any());
        verify(outboxEventPort, never()).record(any(), any(), any(), any());
    }
}
