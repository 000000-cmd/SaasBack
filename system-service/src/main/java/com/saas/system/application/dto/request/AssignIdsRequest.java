package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/** Reusable para "asigna este conjunto de ids" (role-permissions, menu-roles, etc.). */
public record AssignIdsRequest(@NotNull Set<UUID> ids) {}
