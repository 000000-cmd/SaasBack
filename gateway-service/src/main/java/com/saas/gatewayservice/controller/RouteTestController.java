package com.saas.gatewayservice.controller;

import com.saas.gatewayservice.components.RouteValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RouteTestController {

    @Autowired
    private RouteValidator routeValidator;

    @GetMapping("/test-route")
    public Map<String, Object> testRoute(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("path", path);
        response.put("isPublic", routeValidator.isPublicEndpoint(path));
        response.put("requiresAuth", !routeValidator.isPublicEndpoint(path));
        return response;
    }
}