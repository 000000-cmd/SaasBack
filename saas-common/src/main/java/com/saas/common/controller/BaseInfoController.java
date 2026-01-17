package com.saas.common.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.dto.ServiceInfoDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador base que proporciona información del servicio.
 * Los microservicios deben extender esta clase o usar sus métodos.
 */
public abstract class BaseInfoController {

    @Value("${spring.application.name:unknown}")
    protected String applicationName;

    @Value("${spring.profiles.active:default}")
    protected String activeProfile;

    @Autowired(required = false)
    protected BuildProperties buildProperties;

    /**
     * Endpoint para obtener información del servicio
     */
    @GetMapping("/api/info")
    public ResponseEntity<ApiResponse<ServiceInfoDTO>> getServiceInfo() {
        ServiceInfoDTO info = buildServiceInfo();
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * Endpoint simplificado de versión (legacy)
     */
    @GetMapping("/api/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("service", applicationName);
        version.put("version", getServiceVersion());
        version.put("environment", activeProfile);
        return ResponseEntity.ok(version);
    }

    /**
     * Construye el DTO de información del servicio
     */
    protected ServiceInfoDTO buildServiceInfo() {
        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("os.name", System.getProperty("os.name"));
        additionalInfo.put("os.arch", System.getProperty("os.arch"));

        return ServiceInfoDTO.builder()
                .serviceName(applicationName)
                .version(getServiceVersion())
                .environment(activeProfile)
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(SpringBootVersion.getVersion())
                .uptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime())
                .additionalInfo(additionalInfo)
                .build();
    }

    /**
     * Obtiene la versión del servicio
     */
    protected String getServiceVersion() {
        if (buildProperties != null) {
            return buildProperties.getVersion();
        }
        return "No version";
    }
}