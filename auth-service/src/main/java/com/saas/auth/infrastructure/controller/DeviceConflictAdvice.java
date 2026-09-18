package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.response.DeviceConflictResponse;
import com.saas.auth.application.exception.DeviceConflictException;
import com.saas.common.dto.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce el choque de dispositivos a un 409 CON DATOS.
 *
 * <p>Va con precedencia maxima a proposito: {@code GlobalExceptionHandler} tiene
 * un manejador de {@code Exception}, y Spring recorre los advice en orden. Sin
 * este {@code @Order}, el generico podria atrapar primero el choque y devolver
 * un 500 sin nada dentro — con lo que la app no tendria como saber que aparato
 * hay que desvincular.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DeviceConflictAdvice {

    @ExceptionHandler(DeviceConflictException.class)
    public ResponseEntity<ApiResponse<DeviceConflictResponse>> handle(DeviceConflictException ex) {
        DeviceConflictResponse body = new DeviceConflictResponse(ex.getConflicts(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<DeviceConflictResponse>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(body)
                        .status(HttpStatus.CONFLICT.value())
                        .build());
    }
}
