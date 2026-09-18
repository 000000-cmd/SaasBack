package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta o edicion de un cliente del negocio.
 *
 * <p>{@code phone} llega como lo escriba quien sea y se normaliza a E.164 en
 * el servicio, no aqui: la validacion de formato vive en un solo sitio.</p>
 *
 * <p>Nulo = cliente de mostrador, el que llega sin cita. Tiene nombre y nada
 * mas; no recibe mensajes ni puede consultar su cita por codigo publico.</p>
 */
public record BusinessClientRequest(
        @NotNull UUID businessId,
        @NotBlank @Size(max = 160) String displayName,
        @Size(max = 40) String phone,
        UUID thirdPartyId,
        @Size(max = 80) String acquisitionSource,
        @Size(max = 500) String notes
) {}
