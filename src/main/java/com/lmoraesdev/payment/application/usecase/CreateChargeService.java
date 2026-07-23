package com.lmoraesdev.payment.application.usecase;

import com.lmoraesdev.payment.application.port.in.CreateCharge;
import com.lmoraesdev.payment.application.port.in.CreateChargeCommand;
import com.lmoraesdev.payment.application.port.in.CreateChargeResult;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort;
import com.lmoraesdev.payment.application.port.out.IdempotencyPort.StoredIdempotency;
import com.lmoraesdev.payment.domain.exception.IdempotencyConflictException;
import com.lmoraesdev.payment.domain.model.Money;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CreateChargeService implements CreateCharge {
    private final ChargeCreationCoordinator chargeCreationCoordinator;
    private final IdempotencyPort idempotencyPort;

    public CreateChargeService(
            ChargeCreationCoordinator chargeCreationCoordinator, IdempotencyPort idempotencyPort) {
        this.chargeCreationCoordinator = chargeCreationCoordinator;
        this.idempotencyPort = idempotencyPort;
    }

    @Override
    public CreateChargeResult create(CreateChargeCommand command) {
        Money amount = new Money(command.amount());
        String requestHash = hash(command.amount());

        Optional<StoredIdempotency> existing = idempotencyPort.findByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash, command.idempotencyKey());
        }

        try {
            return chargeCreationCoordinator.createAndPersist(command, amount, requestHash);
        } catch (DataIntegrityViolationException e) {
            // A transação da tentativa de criação já sofreu rollback completo (violação de
            // constraint aborta a transação inteira no Postgres). A requisição vencedora já
            // deve ter commitado seu registro de idempotência; buscamos numa transação nova.
            StoredIdempotency winner =
                    idempotencyPort
                            .findByKey(command.idempotencyKey())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "registro de idempotência esperado após"
                                                            + " conflito de constraint não"
                                                            + " encontrado para key: "
                                                            + command.idempotencyKey()));
            return replay(winner, requestHash, command.idempotencyKey());
        }
    }

    private CreateChargeResult replay(StoredIdempotency existing, String requestHash, String key) {
        if (!existing.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(key);
        }

        CreateChargeResult previous = existing.result();
        return new CreateChargeResult(
                previous.id(), previous.status(), previous.amount(), previous.createdAt(), true);
    }

    private String hash(BigDecimal amount) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(amount.toPlainString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
