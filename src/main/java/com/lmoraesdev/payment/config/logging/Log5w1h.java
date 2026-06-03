package com.lmoraesdev.payment.config.logging;

public record Log5w1h(
        String traceId,
        String spanId,
        String where,
        String why,
        String when,
        Object who,
        Object what,
        String how) {}
