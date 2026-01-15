package com.saas.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO genérico para la creación y actualización de cualquier entidad de tipo lista.
 * Contiene las validaciones estándar para los campos comunes.
 */
public class ListTypeRequestDTO {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede superar los 50 caracteres")
    private String code;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String name;

    @NotNull(message = "El orden es obligatorio")
    private Integer order;

    @NotNull(message = "El indicador de habilitado es obligatorio")
    private Boolean indicatorEnabled;

    // --- Getters y Setters ---
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Boolean getIndicatorEnabled() {
        return indicatorEnabled;
    }

    public void setIndicatorEnabled(Boolean indicatorEnabled) {
        this.indicatorEnabled = indicatorEnabled;
    }
}
