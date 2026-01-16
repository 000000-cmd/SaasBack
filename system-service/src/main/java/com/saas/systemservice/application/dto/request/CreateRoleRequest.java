package com.saas.systemservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreateRoleRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String code;

    @NotBlank
    private String name;

    private String description;
}
