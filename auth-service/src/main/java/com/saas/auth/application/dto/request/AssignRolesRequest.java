package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AssignRolesRequest(@NotNull Set<UUID> roleIds) {}
