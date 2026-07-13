package com.saas.business.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta MÍNIMA de un empleado por el dueño: solo la cuenta para el APK. El
 * tercero y el registro laboral nacen como "shells" (solo FKs) y el propio
 * empleado completa sus datos en su primer ingreso al APK — un dueño no se
 * sienta a digitar los datos personales de todo su equipo.
 */
public record EmployeeProvisionRequest(
        @NotNull UUID branchId,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 60) String password
) {}
