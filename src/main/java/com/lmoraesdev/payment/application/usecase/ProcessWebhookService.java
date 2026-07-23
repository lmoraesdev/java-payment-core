package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.ProcessWebhook;
import com.lmoraesdev.payment.application.port.in.ProcessWebhookCommand;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import com.lmoraesdev.payment.application.port.out.WebhookEventPort;
import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.event.ChargeStatusChangedEvent;
import com.lmoraesdev.payment.domain.exception.ChargeNotFoundException;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessWebhookService implements ProcessWebhook {
    private final ChargeRepository chargeRepository;
    private final OutboxEventPort outboxEventPort;
    private final WebhookEventPort webhookEventPort;
    private final Counter webhooksProcessedCounter;

    public ProcessWebhookService(
            ChargeRepository chargeRepository,
            OutboxEventPort outboxEventPort,
            WebhookEventPort webhookEventPort,
            MeterRegistry meterRegistry) {
        this.chargeRepository = chargeRepository;
        this.outboxEventPort = outboxEventPort;
        this.webhookEventPort = webhookEventPort;
        this.webhooksProcessedCounter = meterRegistry.counter("webhooks_processed_total");
    }

    @Override
    @Transactional
    public void process(ProcessWebhookCommand command) {
        webhooksProcessedCounter.increment();

        if (webhookEventPort.existsByEventId(command.eventId())) {
            Logger5w1hBuilder.create(ProcessWebhookService.class)
                    .where("ProcessWebhookService")
                    .what("webhook_already_processed")
                    .why("duplicate webhook event received")
                    .who("system")
                    .how("processWebhook")
                    .info();
            return;
        }

        Charge charge =
                chargeRepository
                        .findById(command.chargeId())
                        .orElseThrow(() -> new ChargeNotFoundException(command.chargeId()));

        ChargeStatus previousStatus = charge.getStatus();
        ChargeStatus newStatus = ChargeStatus.valueOf(command.status());

        charge.transitionTo(newStatus);

        chargeRepository.save(charge);

        String eventType =
                switch (newStatus) {
                    case PAID -> "ChargePaid";
                    case EXPIRED -> "ChargeExpired";
                    case CANCELLED -> "ChargeCancelled";
                    default ->
                            throw new IllegalStateException(
                                    "status inesperado no outbox: " + newStatus);
                };

        outboxEventPort.record(
                "Charge",
                charge.getId().toString(),
                eventType,
                new ChargeStatusChangedEvent(
                        charge.getId(), previousStatus.name(), newStatus.name(), Instant.now()));

        webhookEventPort.save(command.eventId(), command.chargeId());

        Logger5w1hBuilder.create(ProcessWebhookService.class)
                .where("ProcessWebhookService")
                .what("charge_state_transitioned")
                .why("webhook received from provider")
                .who("system")
                .how("processWebhook")
                .info();
    }
}
