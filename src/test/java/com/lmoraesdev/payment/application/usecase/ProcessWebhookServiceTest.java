package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.ProcessWebhookCommand;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.application.port.out.WebhookEventPort;
import com.lmoraesdev.payment.domain.exception.ChargeNotFoundException;
import com.lmoraesdev.payment.domain.exception.InvalidStateTransitionException;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ProcessWebhookService")
@ExtendWith(MockitoExtension.class)
class ProcessWebhookServiceTest {

    @Mock ChargeRepository chargeRepository;

    @Mock OutboxEventPort outboxEventPort;

    @Mock WebhookEventPort webhookEventPort;

    @InjectMocks ProcessWebhookService service;

    @Test
    @DisplayName("evento novo transiciona ACTIVE->PAID, grava outbox e registra dedup")
    void transitionsChargeAndRecordsOutboxAndDedup() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.ACTIVE).build();
        when(webhookEventPort.existsByEventId("event-1")).thenReturn(false);
        when(chargeRepository.findById(charge.getId())).thenReturn(Optional.of(charge));

        service.process(new ProcessWebhookCommand("event-1", charge.getId(), "PAID"));

        assertThat(charge.getStatus()).isEqualTo(ChargeStatus.PAID);
        verify(chargeRepository).save(charge);
        verify(outboxEventPort)
                .record(eq("Charge"), eq(charge.getId().toString()), eq("ChargePaid"), any());
        verify(webhookEventPort).save("event-1", charge.getId());
    }

    @Test
    @DisplayName("evento duplicado não toca em charge nem outbox")
    void ignoresDuplicateEvent() {
        when(webhookEventPort.existsByEventId("event-2")).thenReturn(true);

        service.process(new ProcessWebhookCommand("event-2", UUID.randomUUID(), "PAID"));

        verify(chargeRepository, never()).findById(any());
        verify(chargeRepository, never()).save(any());
        verify(outboxEventPort, never()).record(any(), any(), any(), any());
        verify(webhookEventPort, never()).save(any(), any());
    }

    @Test
    @DisplayName("charge inexistente lança ChargeNotFoundException")
    void throwsChargeNotFoundExceptionWhenChargeMissing() {
        UUID chargeId = UUID.randomUUID();
        when(webhookEventPort.existsByEventId("event-3")).thenReturn(false);
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.process(
                                        new ProcessWebhookCommand("event-3", chargeId, "PAID")))
                .isInstanceOf(ChargeNotFoundException.class);
    }

    @Test
    @DisplayName("transição inválida (charge já PAID) lança InvalidStateTransitionException")
    void throwsInvalidStateTransitionExceptionForInvalidTransition() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.PAID).build();
        when(webhookEventPort.existsByEventId("event-4")).thenReturn(false);
        when(chargeRepository.findById(charge.getId())).thenReturn(Optional.of(charge));

        assertThatThrownBy(
                        () ->
                                service.process(
                                        new ProcessWebhookCommand(
                                                "event-4", charge.getId(), "PAID")))
                .isInstanceOf(InvalidStateTransitionException.class);
        verify(chargeRepository, never()).save(any());
        verify(webhookEventPort, never()).save(any(), any());
    }
}
