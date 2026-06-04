package com.lmoraesdev.payment.application.usecase;

import org.springframework.stereotype.Service;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.Money;

@Service
public class CreateChargeService implements CreateCharge {
  private final ChargeRepository chargeRepository;

  public CreateChargeService(ChargeRepository chargeRepository){
    this.chargeRepository = chargeRepository;
  }

  @Override
  public CreateChargeResult create(CreateChargeCommand command){

    Money amount = new Money(command.amount());

    Charge charge = Charge.create(amount);

    Charge saved = chargeRepository.save(charge);

    return new CreateChargeResult(
        saved.getId(),
        saved.getStatus().name(),
        saved.getAmount().amount(),
        saved.getCreatedAt()
    );

  }
}
