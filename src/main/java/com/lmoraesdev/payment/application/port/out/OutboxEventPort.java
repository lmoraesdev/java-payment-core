package com.lmoraesdev.payment.application.port.out;

public interface OutboxEventPort {

    void record(String aggregateType, String aggregateId, String eventType, Object payload);
}
