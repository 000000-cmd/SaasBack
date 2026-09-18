package com.saas.system.application.dto.response.flow;

import java.util.UUID;

public record FlowMessageView(UUID id, String code, String name, String body,
                              String description, boolean enabled) {}
