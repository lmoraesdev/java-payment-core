package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import com.lmoraesdev.payment.domain.exception.DomainException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    // 422 — regra de negócio. Também esperado: NÃO loga.
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business rule violation");
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
