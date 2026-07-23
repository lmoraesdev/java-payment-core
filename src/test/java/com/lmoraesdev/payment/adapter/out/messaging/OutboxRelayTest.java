package com.lmoraesdev.payment.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("OutboxRelay")
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxClaimCoordinator outboxClaimCoordinator;

    @Mock KafkaTemplate<Object, Object> kafkaTemplate;

    SimpleMeterRegistry meterRegistry;

    OutboxRelay relay;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        relay = new OutboxRelay(outboxClaimCoordinator, kafkaTemplate, meterRegistry);
        logger = (Logger) LoggerFactory.getLogger(OutboxRelay.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("reivindica lote via OutboxClaimCoordinator, publica e marca PUBLISHED")
    void claimsPublishesAndMarksPublished() {
        OutboxEventEntity event =
                OutboxEventEntity.pending("Charge", "charge-1", "ChargeCreated", "{}", null);
        when(outboxClaimCoordinator.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        relay.publishPending();

        verify(outboxClaimCoordinator).markPublished(event.getId());
        verify(outboxClaimCoordinator, never()).revertToPending(any());
        assertThat(meterRegistry.counter("outbox_events_published_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("outbox_events_failed_total").count()).isEqualTo(0.0);
        assertThat(meterRegistry.timer("outbox_publish_lag").count()).isEqualTo(1L);
    }

    @Test
    @DisplayName(
            "falha ao publicar loga via Logger5w1hBuilder e reverte pra PENDING via coordinator")
    void logsFailureAndRevertsToPending() {
        OutboxEventEntity event =
                OutboxEventEntity.pending(
                        "Charge", "charge-3", "ChargeCreated", "{}", "trace-original-request");
        when(outboxClaimCoordinator.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        relay.publishPending();

        verify(outboxClaimCoordinator).revertToPending(event.getId());
        verify(outboxClaimCoordinator, never()).markPublished(any());

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel().toString()).isEqualTo("ERROR");
        assertThat(logged.getFormattedMessage()).startsWith("outbox_publish_failed:");
        assertThat(logged.getThrowableProxy().getMessage()).contains("kafka down");
        assertThat(meterRegistry.counter("outbox_events_failed_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("outbox_events_published_total").count()).isEqualTo(0.0);
        assertThat(logged.getMDCPropertyMap()).containsEntry("traceId", "trace-original-request");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("evento sem correlationId não mexe no MDC ao logar a falha")
    void doesNotTouchMdcWhenCorrelationIdIsAbsent() {
        OutboxEventEntity event =
                OutboxEventEntity.pending("Charge", "charge-6", "ChargeCreated", "{}", null);
        when(outboxClaimCoordinator.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        relay.publishPending();

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getMDCPropertyMap()).doesNotContainKey("traceId");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("lote vazio não publica nem marca nada")
    void doesNothingWhenBatchIsEmpty() {
        when(outboxClaimCoordinator.claimBatch()).thenReturn(List.of());

        relay.publishPending();

        verify(kafkaTemplate, never()).send(any(String.class), any(), any());
        verify(outboxClaimCoordinator, never()).markPublished(any());
        verify(outboxClaimCoordinator, never()).revertToPending(any());
    }

    @SuppressWarnings("unchecked")
    private SendResult<Object, Object> mockSendResult() {
        return org.mockito.Mockito.mock(SendResult.class);
    }
}
