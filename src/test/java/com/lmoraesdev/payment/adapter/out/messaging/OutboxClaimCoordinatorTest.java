package com.lmoraesdev.payment.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventEntity;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxEventJpaRepository;
import com.lmoraesdev.payment.adapter.out.persistence.outbox.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("OutboxClaimCoordinator")
@ExtendWith(MockitoExtension.class)
class OutboxClaimCoordinatorTest {

    @Mock OutboxEventJpaRepository repository;

    OutboxClaimCoordinator coordinator;

    @Nested
    @DisplayName("claimBatch")
    class ClaimBatch {

        @Test
        @DisplayName("reclama eventos IN_FLIGHT travados antes de buscar o próximo lote PENDING")
        void reapsStuckInFlightBeforeFetchingPendingBatch() {
            coordinator = new OutboxClaimCoordinator(repository);
            when(repository.reapStuckInFlight(any(Instant.class))).thenReturn(0);
            when(repository.findBatchForUpdateSkipLocked()).thenReturn(List.of());
            when(repository.saveAll(List.of())).thenReturn(List.of());

            coordinator.claimBatch();

            InOrder inOrder = Mockito.inOrder(repository);
            inOrder.verify(repository).reapStuckInFlight(any(Instant.class));
            inOrder.verify(repository).findBatchForUpdateSkipLocked();
        }

        @Test
        @DisplayName("marca o lote reivindicado como IN_FLIGHT com claimedAt e persiste")
        void marksFetchedBatchInFlightAndPersists() {
            coordinator = new OutboxClaimCoordinator(repository);
            OutboxEventEntity event =
                    OutboxEventEntity.pending("Charge", "charge-1", "ChargeCreated", "{}", null);
            when(repository.reapStuckInFlight(any(Instant.class))).thenReturn(0);
            when(repository.findBatchForUpdateSkipLocked()).thenReturn(List.of(event));
            when(repository.saveAll(List.of(event)))
                    .thenAnswer(
                            invocation -> {
                                assertThat(event.getStatus()).isEqualTo(OutboxStatus.IN_FLIGHT);
                                assertThat(event.getClaimedAt()).isNotNull();
                                return List.of(event);
                            });

            List<OutboxEventEntity> claimed = coordinator.claimBatch();

            assertThat(claimed).containsExactly(event);
        }
    }

    @Nested
    @DisplayName("markPublished")
    class MarkPublished {

        @Test
        @DisplayName("marca o evento existente como PUBLISHED e persiste")
        void marksExistingEventPublished() {
            coordinator = new OutboxClaimCoordinator(repository);
            OutboxEventEntity event =
                    OutboxEventEntity.pending("Charge", "charge-2", "ChargeCreated", "{}", null);
            when(repository.findById(event.getId())).thenReturn(Optional.of(event));

            coordinator.markPublished(event.getId());

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            verify(repository).save(event);
        }

        @Test
        @DisplayName("não faz nada quando o evento não é encontrado")
        void doesNothingWhenEventNotFound() {
            coordinator = new OutboxClaimCoordinator(repository);
            UUID missingId = UUID.randomUUID();
            when(repository.findById(missingId)).thenReturn(Optional.empty());

            coordinator.markPublished(missingId);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("revertToPending")
    class RevertToPending {

        @Test
        @DisplayName("reverte o evento existente para PENDING e limpa claimedAt")
        void revertsExistingEventToPendingAndClearsClaimedAt() {
            coordinator = new OutboxClaimCoordinator(repository);
            OutboxEventEntity event =
                    OutboxEventEntity.pending("Charge", "charge-3", "ChargeCreated", "{}", null);
            event.markInFlight();
            when(repository.findById(event.getId())).thenReturn(Optional.of(event));

            coordinator.revertToPending(event.getId());

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getClaimedAt()).isNull();
            verify(repository).save(event);
        }

        @Test
        @DisplayName("não faz nada quando o evento não é encontrado")
        void doesNothingWhenEventNotFound() {
            coordinator = new OutboxClaimCoordinator(repository);
            UUID missingId = UUID.randomUUID();
            when(repository.findById(missingId)).thenReturn(Optional.empty());

            coordinator.revertToPending(missingId);

            verify(repository, never()).save(any());
        }
    }
}
