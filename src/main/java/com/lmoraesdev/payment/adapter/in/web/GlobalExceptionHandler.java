package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.exception.ChargeNotFoundException;
import com.lmoraesdev.payment.domain.exception.DomainException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — validação. Erro esperado do cliente: NÃO loga.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setTitle("Validation failed");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        problem.setProperty("errors", errors);
        addTraceId(problem);
        return problem;
    }

    // 400 — header obrigatório ausente. Erro esperado do cliente: NÃO loga.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Header obrigatório ausente: %s".formatted(ex.getHeaderName()));
        problem.setTitle("Missing required header");
        addTraceId(problem);
        return problem;
    }

    // 404 — recurso não encontrado. Esperado: NÃO loga.
    @ExceptionHandler(ChargeNotFoundException.class)
    public ProblemDetail handleChargeNotFound(ChargeNotFoundException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Charge not found");
        addTraceId(problem);
        return problem;
    }

    // 422 — regra de negócio. Também esperado: NÃO loga.
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business rule violation");
        addTraceId(problem);
        return problem;
    }

    // 409 — conflito de concorrência otimista. Recuperável: cliente/provedor pode tentar de novo.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        Logger5w1hBuilder.create(GlobalExceptionHandler.class)
                .where("GlobalExceptionHandler")
                .what("optimistic_lock_conflict")
                .why("recurso modificado concorrentemente, quem chamou deve tentar de novo")
                .who("system")
                .how("exception handling")
                .error(ex);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Recurso modificado concorrentemente, tente novamente");
        problem.setTitle("Concurrent modification conflict");
        addTraceId(problem);
        return problem;
    }

    // 500 — inesperado. AQUI sim loga, em ERROR, com a stack.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        Logger5w1hBuilder.create(GlobalExceptionHandler.class)
                .where("GlobalExceptionHandler")
                .what("unexpected_error")
                .why("unhandled_exception")
                .who("system")
                .how("exception handling")
                .error(ex);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
        problem.setTitle("Internal error");
        addTraceId(problem);
        return problem;
    }

    private void addTraceId(ProblemDetail problem) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }
}
