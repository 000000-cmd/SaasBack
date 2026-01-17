package com.saas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para información del servicio.
 * Usado por el endpoint /api/info de cada microservicio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInfoDTO {

    private String serviceName;
    private String version;
    private String environment;
    private String javaVersion;
    private String springBootVersion;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Long uptimeMillis;
    private Map<String, String> additionalInfo;
}