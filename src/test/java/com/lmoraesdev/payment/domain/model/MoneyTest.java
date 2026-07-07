package com.lmoraesdev.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmoraesdev.payment.domain.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Money")
class MoneyTest {

    record Case(String name, String amount, boolean valid) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> cases() {
        return Stream.of(
                new Case("aceita centavo mínimo", "0.01", true),
                new Case("aceita valor comum", "10.50", true),
                new Case("aceita valor alto", "9999999.99", true),
                new Case("rejeita zero", "0.00", false),
                new Case("rejeita negativo", "-1.00", false),
                new Case("rejeita negativo pequeno", "-0.01", false));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void validates(Case c) {
        BigDecimal value = new BigDecimal(c.amount());
        if (c.valid()) {
            assertThatNoException().isThrownBy(() -> new Money(value));
        } else {
            assertThatThrownBy(() -> new Money(value)).isInstanceOf(InvalidAmountException.class);
        }
    }

    @Test
    @DisplayName("rejeita amount nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> new Money(null)).isInstanceOf(InvalidAmountException.class);
    }
}
