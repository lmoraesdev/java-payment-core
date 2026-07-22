package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.adapter.in.web.documentation.ChargeControllerDoc;
import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.in.GetCharge;
import com.lmoraesdev.payment.application.port.in.GetChargeResult;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChargeController implements ChargeControllerDoc {
    private final CreateCharge createCharge;
    private final GetCharge getCharge;

    public ChargeController(CreateCharge createCharge, GetCharge getCharge) {
        this.createCharge = createCharge;
        this.getCharge = getCharge;
    }

    @Override
    public ResponseEntity<CreateChargeResponse> create(CreateChargeRequest request) {
        CreateChargeResult result = createCharge.create(new CreateChargeCommand(request.amount()));

        CreateChargeResponse response =
                new CreateChargeResponse(
                        result.id(), result.status(), result.amount(), result.createdAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<GetChargeResponse> get(UUID id) {
        GetChargeResult result = getCharge.find(id);

        GetChargeResponse response =
                new GetChargeResponse(
                        result.id(), result.status(), result.amount(), result.createdAt());

        return ResponseEntity.ok(response);
    }
}
