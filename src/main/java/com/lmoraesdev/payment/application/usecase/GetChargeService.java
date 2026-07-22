package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.GetCharge;
import com.lmoraesdev.payment.application.port.in.GetChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.exception.ChargeNotFoundException;
import com.lmoraesdev.payment.domain.model.Charge;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetChargeService implements GetCharge {
    private final ChargeRepository chargeRepository;

    public GetChargeService(ChargeRepository chargeRepository) {
        this.chargeRepository = chargeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetChargeResult find(UUID id) {
        Charge charge =
                chargeRepository.findById(id).orElseThrow(() -> new ChargeNotFoundException(id));

        return new GetChargeResult(
                charge.getId(),
                charge.getStatus().name(),
                charge.getAmount().amount(),
                charge.getCreatedAt());
    }
}
