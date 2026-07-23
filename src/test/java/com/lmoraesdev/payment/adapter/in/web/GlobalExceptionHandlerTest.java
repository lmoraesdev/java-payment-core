package com.lmoraesdev.payment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("OptimisticLockingFailureException vira 409 Conflict")
    void mapsOptimisticLockingFailureExceptionTo409() {
        ProblemDetail problem =
                handler.handleOptimisticLock(new OptimisticLockingFailureException("stale row"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
