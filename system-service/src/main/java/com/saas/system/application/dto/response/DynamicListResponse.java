package com.saas.system.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO genérico de respuesta para items de cualquier lista dinámica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DynamicListResponse {

    private String id;
    private String code;
    private String name;
    private Integer displayOrder;
    private Boolean enabled;
    private LocalDateTime auditDate;
}