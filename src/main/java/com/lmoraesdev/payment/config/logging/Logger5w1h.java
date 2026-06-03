package com.lmoraesdev.payment.config.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logger5w1h {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Logger logger;

    private Logger5w1h(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static Logger5w1h of(Class<?> clazz) {
        return new Logger5w1h(clazz);
    }

    public void info(Log5w1h data) {
        logger.info(toJson(data));
    }

    public void debug(Log5w1h data) {
        logger.debug(toJson(data));
    }

    public void warn(Log5w1h data) {
        logger.warn(toJson(data));
    }

    public void error(Log5w1h data, Throwable exception) {
        logger.error(toJson(data), exception);
    }

    private String toJson(Log5w1h data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception exception) {
            return data.toString();
        }
    }
}
