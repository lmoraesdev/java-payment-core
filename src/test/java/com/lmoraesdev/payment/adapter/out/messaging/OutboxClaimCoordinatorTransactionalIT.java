package com.lmoraesdev.payment.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxStatus;
import com.lmoraesdev.payment.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Propagation.NOT_SUPPORTED desliga o wrapper transacional do próprio teste, senão o rollback
// do @Transactional real de claimBatch() ficaria preso na mesma transação nunca-commitada do teste.
@DisplayName("OutboxClaimCoordinator (transacional real)")
@Import(OutboxClaimCoordinator.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxClaimCoordinatorTransactionalIT extends AbstractIntegrationTest {

    @Autowired OutboxClaimCoordinator coordinator;

    @MockitoSpyBean OutboxEventJpaRepository repository;

    private OutboxEventEntity seeded;

    @AfterEach
    void cleanUp() {
        if (seeded != null) {
            repository.deleteById(seeded.getId());
        }
    }

    @Test
    @DisplayName("exceção no meio de claimBatch desfaz a mudança de status pro evento reivindicado")
    void exceptionMidClaimBatchRollsBackStatusChange() {
        seeded =
                repository.save(
                        OutboxEventEntity.pending(
                                "Charge", "aggregate-rollback", "ChargeCreated", "{}", null));

        doThrow(new RuntimeException("boom-mid-claim")).when(repository).saveAll(anyList());

        assertThatThrownBy(() -> coordinator.claimBatch())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom-mid-claim");

        OutboxEventEntity reloaded = repository.findById(seeded.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(reloaded.getClaimedAt()).isNull();
    }
}
