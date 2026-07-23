package com.lmoraesdev.payment.adapter.out.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@DisplayName("OutboxEventAdapter")
@ExtendWith(MockitoExtension.class)
class OutboxEventAdapterTest {

    @Mock OutboxEventJpaRepository repository;

    OutboxEventAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OutboxEventAdapter(repository, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("captura o traceId do MDC e grava junto com o evento")
    void capturesCorrelationIdFromMdc() {
        MDC.put("traceId", "trace-123");

        adapter.record("Charge", "charge-1", "ChargeCreated", Map.of("amount", "10.00"));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isEqualTo("trace-123");
    }

    @Test
    @DisplayName("grava correlationId nulo quando não há traceId no MDC")
    void recordsNullCorrelationIdWhenMdcIsEmpty() {
        adapter.record("Charge", "charge-2", "ChargeCreated", Map.of("amount", "20.00"));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isNull();
    }
}
