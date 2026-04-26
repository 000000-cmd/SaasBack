package com.saas.gatewayservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class GatewayInfoController {

    @Value("${spring.application.name:gateway-service}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String profile;

    @GetMapping("/api/info")
    public Mono<Map<String, Object>> info() {
        return Mono.just(Map.of(
                "service", appName,
                "profile", profile,
                "javaVersion", System.getProperty("java.version"),
                "os", System.getProperty("os.name") + " " + System.getProperty("os.arch")
        ));
    }
}
