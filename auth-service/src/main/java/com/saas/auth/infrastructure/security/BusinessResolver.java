package com.saas.auth.infrastructure.security;

import com.saas.auth.infrastructure.client.BusinessServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resuelve el businessId del dueño para sellarlo en el JWT y exponerlo en
 * {@code /users/me}. NUNCA rompe el login: si business-service no responde o el
 * usuario aún no tiene negocio, devuelve null y el token se emite sin el claim
 * (fallback al comportamiento previo).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessResolver {

    private final BusinessServiceClient client;

    public UUID resolve(UUID userId) {
        if (userId == null) return null;
        try {
            BusinessServiceClient.OwnerBusinessDto dto = client.ownerBusiness(userId);
            return dto == null ? null : dto.businessId();
        } catch (Exception ex) {
            log.debug("No se pudo resolver businessId para userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }
}
