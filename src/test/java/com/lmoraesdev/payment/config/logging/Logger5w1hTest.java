package com.lmoraesdev.payment.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("Logger5w1h")
class Logger5w1hTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(Logger5w1hTest.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("info() grava mensagem formatada com what e why, nunca null")
    void infoLogsFormattedMessageNotNull() {
        Logger5w1h.of(Logger5w1hTest.class)
                .info(new Log5w1h("where", "why happened", "who", "charge_created", "how"));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("charge_created: why happened");
    }

    @Test
    @DisplayName("error() define a causa e mantém a mensagem formatada")
    void errorLogsFormattedMessageAndCause() {
        RuntimeException cause = new RuntimeException("boom");

        Logger5w1h.of(Logger5w1hTest.class)
                .error(
                        new Log5w1h(
                                "where", "unhandled_exception", "who", "unexpected_error", "how"),
                        cause);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("unexpected_error: unhandled_exception");
        assertThat(appender.list.get(0).getThrowableProxy().getMessage()).isEqualTo("boom");
    }
}
