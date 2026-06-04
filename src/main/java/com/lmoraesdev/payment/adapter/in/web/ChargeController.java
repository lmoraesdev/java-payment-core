package com.lmoraesdev.payment.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/charges")
public class ChargeController {
  private final CreateCharge createCharge;

  public ChargeController(CreateCharge createCharge){
    this.createCharge = createCharge;
  }

  @PostMapping
  public ResponseEntity<CreateChargeResponse> create(@Valid @RequestBody CreateChargeRequest request){
        CreateChargeResult result = createCharge.create(new CreateChargeCommand(request.amount()));

        CreateChargeResponse response = new CreateChargeResponse(
            result.id(),
            result.status(),
            result.amount(),
            result.createdAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
