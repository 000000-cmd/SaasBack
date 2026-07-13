package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cliente S2S a thirdparty-service para dar de alta la persona (tercero) durante
 * el aprovisionamiento del negocio. Usa el endpoint interno {@code /internal/**}
 * (sin JWT), llamado directo via Eureka (no por el gateway).
 *
 * <p>{@code path = "/thirdparty"} = context-path del servicio.</p>
 */
@FeignClient(name = "thirdparty-service", contextId = "thirdparty-internal-business", path = "/thirdparty")
public interface ThirdPartyClient {

    @PostMapping("/internal/third-parties")
    PersonResponse createPerson(@RequestBody CreatePersonRequest request);

    /** Actualiza la persona existente (cuando el usuario ya tenía tercero). */
    @PutMapping("/internal/third-parties/{id}")
    PersonResponse updatePerson(@PathVariable("id") UUID id, @RequestBody CreatePersonRequest request);

    /** Resuelve la persona vinculada a una cuenta. Lanza 404 (FeignException) si no existe. */
    @GetMapping("/internal/third-parties/by-user/{userId}")
    PersonResponse personByUser(@PathVariable("userId") UUID userId);

    /** Nombres de personas en lote (id -> nombre completo). */
    @PostMapping("/internal/third-parties/names")
    Map<String, String> personNames(@RequestBody Set<UUID> ids);

    /** Tarjetas de persona en lote (id -> nombre + foto de perfil). */
    @PostMapping("/internal/third-parties/cards")
    Map<String, PersonCard> personCards(@RequestBody Set<UUID> ids);

    /** Pre-check de duplicado de documento antes de orquestar un alta. */
    @GetMapping("/internal/third-parties/document/exists")
    Map<String, Boolean> documentExists(@org.springframework.web.bind.annotation.RequestParam("documentTypeId") UUID documentTypeId,
                                        @org.springframework.web.bind.annotation.RequestParam("documentNumber") String documentNumber);

    /** Espejo de ThirdPartyRequest (solo los campos que el aprovisionamiento envia). */
    record CreatePersonRequest(
            UUID documentTypeId,
            String documentNumber,
            UUID userId,
            UUID businessId,
            String firstName,
            String secondName,
            String firstLastName,
            String secondLastName,
            UUID genderId,
            LocalDate birthDate,
            String photoUrl
    ) {}

    /** Solo se necesita el id del tercero creado. */
    record PersonResponse(UUID id) {}

    /** Espejo del PersonCard interno de thirdparty (nombre + foto). */
    record PersonCard(String fullName, String photoUrl) {}
}
