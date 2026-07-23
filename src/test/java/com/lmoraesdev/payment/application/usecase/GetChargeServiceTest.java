package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.GetChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.exception.ChargeNotFoundException;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("GetChargeService")
@ExtendWith(MockitoExtension.class)
class GetChargeServiceTest {

    @Mock ChargeRepository chargeRepository;

    @InjectMocks GetChargeService service;

    @Test
    @DisplayName("retorna result correspondente à charge encontrada")
    void returnsResultForExistingCharge() {
        Charge charge = ChargeTestData.aCharge().withStatus(ChargeStatus.PAID).build();
        when(chargeRepository.findById(eq(charge.getId()))).thenReturn(Optional.of(charge));

        GetChargeResult result = service.find(charge.getId());

        assertThat(result.id()).isEqualTo(charge.getId());
        assertThat(result.status()).isEqualTo(ChargeStatus.PAID.name());
        assertThat(result.amount()).isEqualByComparingTo(charge.getAmount().amount());
        assertThat(result.createdAt()).isEqualTo(charge.getCreatedAt());
    }

    @Test
    @DisplayName("lança ChargeNotFoundException quando a charge não existe")
    void throwsChargeNotFoundExceptionWhenMissing() {
        UUID id = UUID.randomUUID();
        when(chargeRepository.findById(eq(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(id)).isInstanceOf(ChargeNotFoundException.class);
    }
}
