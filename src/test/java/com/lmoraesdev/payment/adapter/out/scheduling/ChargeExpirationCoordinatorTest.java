package com.lmoraesdev.payment.adapter.out.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChargeExpirationCoordinator")
@ExtendWith(MockitoExtension.class)
class ChargeExpirationCoordinatorTest {

    @Mock ChargeRepository chargeRepository;

    @Mock OutboxEventPort outboxEventPort;

    @InjectMocks ChargeExpirationCoordinator coordinator;

    @Test
    @DisplayName("expireOne transiciona pra EXPIRED, salva e grava outbox")
    void expireOneTransitionsSavesAndRecordsOutbox() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        when(chargeRepository.save(charge)).thenReturn(charge);

        coordinator.expireOne(charge);

        assertThat(charge.getStatus()).isEqualTo(ChargeStatus.EXPIRED);
        verify(chargeRepository).save(charge);
        verify(outboxEventPort)
                .record(
                        eq("Charge"),
                        eq(charge.getId().toString()),
                        eq("ChargeExpired"),
                        ArgumentMatchers.any());
    }
}
