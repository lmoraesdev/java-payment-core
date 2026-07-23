package com.lmoraesdev.payment.application.port.in;

import java.util.UUID;

public record ProcessWebhookCommand(String eventId, UUID chargeId, String status) {}
