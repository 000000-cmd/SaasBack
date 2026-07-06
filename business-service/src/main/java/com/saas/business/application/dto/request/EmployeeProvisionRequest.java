package com.saas.business.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta COMPLETA de un empleado por el dueño (dashboard web): crea la cuenta
 * (auth, rol EMPLOYEE), la persona (thirdparty) y el registro laboral (aqui).
 * El empleado completa el resto de sus datos en su primer ingreso al APK.
 */
public record EmployeeProvisionRequest(
        // Laboral
        @NotNull UUID branchId,
        @NotNull UUID positionId,
        @NotNull LocalDate hireDate,
        @Size(max = 40) String employeeCode,

        // Persona
        @NotNull UUID documentTypeId,
        @NotBlank @Size(max = 40) String documentNumber,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String secondName,
        @NotBlank @Size(max = 80) String firstLastName,
        @Size(max = 80) String secondLastName,
        UUID genderId,
        LocalDate birthDate,

        // Cuenta (para el APK)
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Size(min = 8, max = 60) String password
) {}
