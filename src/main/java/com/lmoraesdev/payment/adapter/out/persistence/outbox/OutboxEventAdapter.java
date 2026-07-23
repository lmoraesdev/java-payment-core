package com.lmoraesdev.payment.adapter.out.persistence.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmoraesdev.payment.application.port.out.OutboxEventPort;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventAdapter implements OutboxEventPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(String aggregateType, String aggregateId, String eventType, Object payload) {
        OutboxEventEntity event =
                OutboxEventEntity.pending(
                        aggregateType, aggregateId, eventType, toJson(payload), MDC.get("traceId"));
        repository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize outbox payload", e);
        }
    }
}
