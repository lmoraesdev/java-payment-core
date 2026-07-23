package com.lmoraesdev.payment.adapter.out.persistence.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyPersistenceAdapter implements IdempotencyPort {

    private final IdempotencyRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyPersistenceAdapter(
            IdempotencyRecordJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<StoredIdempotency> findByKey(String key) {
        return repository
                .findByKey(key)
                .map(
                        entity ->
                                new StoredIdempotency(
                                        entity.getRequestHash(),
                                        toResult(entity.getResponseBody())));
    }

    @Override
    public void save(String key, String requestHash, UUID chargeId, CreateChargeResult result) {
        repository.save(IdempotencyRecordEntity.create(key, requestHash, chargeId, toJson(result)));
    }

    private String toJson(CreateChargeResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize idempotency response", e);
        }
    }

    private CreateChargeResult toResult(String json) {
        try {
            return objectMapper.readValue(json, CreateChargeResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to deserialize idempotency response", e);
        }
    }
}
