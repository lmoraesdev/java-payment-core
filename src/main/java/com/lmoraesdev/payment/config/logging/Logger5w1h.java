package com.lmoraesdev.payment.config.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

public class Logger5w1h {

    private final Logger logger;

    private Logger5w1h(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static Logger5w1h of(Class<?> clazz) {
        return new Logger5w1h(clazz);
    }

    public void info(Log5w1h data) {
        withFields(logger.atInfo(), data).log();
    }

    public void debug(Log5w1h data) {
        withFields(logger.atDebug(), data).log();
    }

    public void warn(Log5w1h data) {
        withFields(logger.atWarn(), data).log();
    }

    public void error(Log5w1h data, Throwable cause) {
        withFields(logger.atError(), data).setCause(cause).log();
    }

    private LoggingEventBuilder withFields(LoggingEventBuilder builder, Log5w1h data) {
        return builder.addKeyValue("where", data.where())
                .addKeyValue("why", data.why())
                .addKeyValue("who", data.who())
                .addKeyValue("what", data.what())
                .addKeyValue("how", data.how());
    }
}
