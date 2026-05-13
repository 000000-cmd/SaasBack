package com.saas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO de informacion del servicio expuesto en /api/info.
 *
 * Estado UP/DOWN se basa en el agregado de dependencias: si todas las
 * dependencias requeridas estan UP, el servicio reporta UP.
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
    private String status;          // UP | DEGRADED | DOWN

    private String javaVersion;
    private String springBootVersion;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Long uptimeMillis;
    private LocalDateTime buildTime;

    /** Dependencias chequeadas (DB, Kafka, Eureka, Redis, etc.). */
    private List<DependencyStatus> dependencies;

    private Map<String, String> additionalInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DependencyStatus {
        private String name;
        private String type;        // DB | KAFKA | EUREKA | REDIS | OTHER
        private String status;      // UP | DOWN | UNKNOWN
        private String detail;      // mensaje breve cuando aplica
    }
}
