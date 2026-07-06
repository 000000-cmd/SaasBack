package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;
import java.util.UUID;

/**
 * Cliente S2S a auth-service para crear cuentas durante orquestaciones
 * (alta de empleados por el dueño). Endpoint interno sin JWT, via Eureka.
 */
@FeignClient(name = "auth-service", contextId = "auth-internal-business", path = "/auth")
public interface AuthClient {

    @PostMapping("/internal/users")
    CreatedUser createUser(@RequestBody CreateUserRequest request);

    /** Los roles van por CODIGO (regla del proyecto); auth resuelve el id sembrado. */
    record CreateUserRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            Set<String> roleCodes
    ) {}

    record CreatedUser(UUID id, String username, String email) {}
}
