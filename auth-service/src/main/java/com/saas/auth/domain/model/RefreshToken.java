package com.saas.auth.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "token")
public class RefreshToken extends BaseDomain {

    private UUID userId;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isExpired() && !isRevoked() && Boolean.TRUE.equals(getEnabled());
    }
}
