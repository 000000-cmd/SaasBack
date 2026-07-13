package com.saas.business.application.dto.response;

import java.util.UUID;

/** businessId del dueño (null si aún no aprovisionó). Respuesta S2S liviana. */
public record OwnerBusinessResponse(UUID businessId) {}
