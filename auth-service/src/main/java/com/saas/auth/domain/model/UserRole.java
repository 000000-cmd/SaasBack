package com.saas.auth.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Asignacion logica Usuario-Rol (espejo de {@code user_role}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserRole extends BaseDomain {

    private UUID userId;
    private UUID roleId;
}
