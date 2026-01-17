package com.saas.system.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para definiciones de listas del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListDefinitionResponse {

    private String id;
    private String displayName;
    private String physicalTableName;

    /**
     * Identificador amigable derivado del nombre de tabla.
     * Ejemplo: sys_list_document_types -> document-types
     */
    private String listType;

    private Boolean enabled;
    private LocalDateTime auditDate;
}