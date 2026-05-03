package com.saas.system.application.dto.event;

import com.saas.system.domain.model.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoleEventPayload {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private Boolean enabled;

    public static RoleEventPayload from(Role r) {
        return RoleEventPayload.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .enabled(r.getEnabled())
                .build();
    }
}
