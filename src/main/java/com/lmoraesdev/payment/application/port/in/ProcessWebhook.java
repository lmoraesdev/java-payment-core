package com.lmoraesdev.payment.application.port.in;

public interface ProcessWebhook {
    void process(ProcessWebhookCommand command);
}
