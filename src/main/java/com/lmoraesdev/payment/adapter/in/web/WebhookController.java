package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.adapter.in.web.documentation.WebhookControllerDoc;
import com.lmoraesdev.payment.application.port.in.ProcessWebhook;
import com.lmoraesdev.payment.application.port.in.ProcessWebhookCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController implements WebhookControllerDoc {
    private final ProcessWebhook processWebhook;

    public WebhookController(ProcessWebhook processWebhook) {
        this.processWebhook = processWebhook;
    }

    @Override
    public ResponseEntity<Void> receive(WebhookProviderPayload payload) {
        processWebhook.process(
                new ProcessWebhookCommand(payload.eventId(), payload.chargeId(), payload.status()));

        return ResponseEntity.ok().build();
    }
}
