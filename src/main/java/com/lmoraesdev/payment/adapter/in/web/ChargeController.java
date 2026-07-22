package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.adapter.in.web.documentation.ChargeControllerDoc;
import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChargeController implements ChargeControllerDoc {
    private final CreateCharge createCharge;

    public ChargeController(CreateCharge createCharge) {
        this.createCharge = createCharge;
    }

    @Override
    public ResponseEntity<CreateChargeResponse> create(CreateChargeRequest request) {
        CreateChargeResult result = createCharge.create(new CreateChargeCommand(request.amount()));

        CreateChargeResponse response =
                new CreateChargeResponse(
                        result.id(), result.status(), result.amount(), result.createdAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
