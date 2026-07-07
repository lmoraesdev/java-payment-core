package com.lmoraesdev.payment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmoraesdev.payment.application.port.out.ChargeRepository;
import com.lmoraesdev.payment.domain.model.Charge;
import com.lmoraesdev.payment.support.AbstractIntegrationTest;
import com.lmoraesdev.payment.testdata.ChargeTestData;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(ChargeRepositoryAdapter.class)
@DisplayName("ChargeRepositoryIT")
class ChargeRepositoryIT extends AbstractIntegrationTest {

    @Autowired ChargeRepository chargeRepository;

    @Test
    @DisplayName("save e findById preservam todos os campos")
    void roundTripPreservesAllFields() {
        Charge charge = ChargeTestData.aCharge().build();

        Charge saved = chargeRepository.save(charge);
        Optional<Charge> found = chargeRepository.findById(saved.getId());

        assertThat(found).isPresent();
        Charge result = found.get();
        assertThat(result.getId()).isEqualTo(charge.getId());
        assertThat(result.getAmount().amount()).isEqualByComparingTo(charge.getAmount().amount());
        assertThat(result.getStatus()).isEqualTo(charge.getStatus());
        assertThat(result.getCreatedAt()).isEqualTo(charge.getCreatedAt());
    }

    @Test
    @DisplayName("findById retorna vazio para id inexistente")
    void findByIdReturnsEmptyForUnknownId() {
        Optional<Charge> found = chargeRepository.findById(java.util.UUID.randomUUID());

        assertThat(found).isEmpty();
    }
}
