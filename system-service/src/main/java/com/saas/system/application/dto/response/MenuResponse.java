package com.saas.system.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para Menú.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponse {

    private String id;
    private String code;
    private String label;
    private String routerLink;
    private String icon;
    private Integer displayOrder;
    private String parentId;
    private Boolean enabled;
    private LocalDateTime auditDate;

    // Para estructura jerárquica
    private List<MenuResponse> children;
}