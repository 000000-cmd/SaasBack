package com.saas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO estándar para respuestas de API.
 * Proporciona una estructura consistente para todas las respuestas.
 *
 * @param <T> Tipo de datos de la respuesta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // === Factory Methods ===

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operación exitosa")
                .data(data)
                .status(200)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(200)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Recurso creado exitosamente")
                .data(data)
                .status(201)
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(201)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .build();
    }

    public static ApiResponse<Map<String, String>> validationError(Map<String, String> errors) {
        return ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Error de validación")
                .data(errors)
                .status(400)
                .build();
    }

    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operación completada")
                .status(204)
                .build();
    }
}