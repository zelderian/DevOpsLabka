package com.example.websecurity26.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {
    private final String version;

    public HealthController(@Value("${app.version:dev}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", version,
                "instance", System.getenv().getOrDefault("HOSTNAME", "local"));
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("version", version);
    }
}
