package com.lmoraesdev.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmoraesdev.payment.adapter.out.persistence.SpringDataChargeRepository;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("CreateChargeService (concorrência real via Postgres)")
class CreateChargeServiceConcurrencyIT {

    @Autowired CreateChargeService createChargeService;

    @Autowired SpringDataChargeRepository springDataChargeRepository;

    @Test
    @DisplayName(
            "duas requisições concorrentes com a mesma Idempotency-Key nova: exatamente uma"
                    + " charge criada, a perdedora recebe o replay da vencedora")
    void concurrentRequestsWithSameNewIdempotencyKeyResultInSingleChargeAndReplay()
            throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateChargeCommand command =
                new CreateChargeCommand(new BigDecimal("100.00"), idempotencyKey);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<CreateChargeResult>> futures =
                    IntStream.range(0, 2)
                            .mapToObj(
                                    i ->
                                            executor.submit(
                                                    () -> {
                                                        barrier.await();
                                                        return createChargeService.create(command);
                                                    }))
                            .toList();

            CreateChargeResult first = futures.get(0).get(10, TimeUnit.SECONDS);
            CreateChargeResult second = futures.get(1).get(10, TimeUnit.SECONDS);

            assertThat(first.id()).isEqualTo(second.id());
            assertThat(first.replayed() ^ second.replayed())
                    .as("exatamente uma das duas respostas deve ser um replay")
                    .isTrue();
            assertThat(springDataChargeRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
