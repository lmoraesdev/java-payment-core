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
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxStatus;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("OutboxRelay")
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxEventJpaRepository repository;

    @Mock KafkaTemplate<Object, Object> kafkaTemplate;

    @InjectMocks OutboxRelay relay;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(OutboxRelay.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("publica evento pendente com sucesso e marca como PUBLISHED")
    void publishesPendingEventSuccessfully() {
        OutboxEventEntity event =
                OutboxEventEntity.pending("Charge", "charge-1", "ChargeCreated", "{}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        relay.publishPending();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        verify(repository).save(event);
    }

    @Test
    @DisplayName(
            "falha ao publicar loga via Logger5w1hBuilder com what=outbox_publish_failed e não marca published")
    void logsFailureViaLogger5w1hBuilder() {
        OutboxEventEntity event =
                OutboxEventEntity.pending("Charge", "charge-2", "ChargeCreated", "{}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        relay.publishPending();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(repository, never()).save(any());

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel().toString()).isEqualTo("ERROR");
        assertThat(logged.getFormattedMessage()).startsWith("outbox_publish_failed:");
        assertThat(logged.getThrowableProxy().getMessage()).contains("kafka down");
    }

    @SuppressWarnings("unchecked")
    private SendResult<Object, Object> mockSendResult() {
        return org.mockito.Mockito.mock(SendResult.class);
    }
}
