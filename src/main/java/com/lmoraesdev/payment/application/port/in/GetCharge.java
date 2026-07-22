package com.lmoraesdev.payment.application.port.in;

import java.util.UUID;

public interface GetCharge {
    GetChargeResult find(UUID id);
}
