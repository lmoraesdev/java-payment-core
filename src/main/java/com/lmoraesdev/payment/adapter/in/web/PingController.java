package com.lmoraesdev.payment.adapter.in.web;

import com.lmoraesdev.payment.config.logging.Logger5w1hBuilder;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Logger5w1hBuilder.create(PingController.class)
                .where("PingController")
                .what("ping")
                .why("smoke_test")
                .who("system")
                .how("GET /ping")
                .info();

        return Map.of("status", "pong");
    }
}
