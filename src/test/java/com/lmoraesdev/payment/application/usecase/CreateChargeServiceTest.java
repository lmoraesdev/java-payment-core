package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
import com.lmoraesdev.payment.domain.model.ChargeStatus;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CreateChargeService")
@ExtendWith(MockitoExtension.class)
class CreateChargeServiceTest {

    @Mock ChargeRepository chargeRepository;

    @InjectMocks CreateChargeService service;

    record Case(String name, String amount) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> validAmounts() {
        return Stream.of(
                new Case("centavo mínimo", "0.01"),
                new Case("valor comum", "100.00"),
                new Case("valor alto", "50000.00"));
    }

    @ParameterizedTest
    @MethodSource("validAmounts")
    @DisplayName("cria cobrança e retorna result com status ACTIVE")
    void createsChargeSuccessfully(Case c) {
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateChargeResult result =
                service.create(new CreateChargeCommand(new BigDecimal(c.amount())));

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(ChargeStatus.ACTIVE.name());
        assertThat(result.amount()).isEqualByComparingTo(c.amount());
        assertThat(result.createdAt()).isNotNull();
        verify(chargeRepository).save(any());
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount zero ou negativo")
    void propagatesExceptionForInvalidAmount() {
        assertThatThrownBy(() -> service.create(new CreateChargeCommand(BigDecimal.ZERO)))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("propaga InvalidAmountException para amount nulo")
    void propagatesExceptionForNullAmount() {
        assertThatThrownBy(() -> service.create(new CreateChargeCommand(null)))
                .isInstanceOf(InvalidAmountException.class);
    }
}
