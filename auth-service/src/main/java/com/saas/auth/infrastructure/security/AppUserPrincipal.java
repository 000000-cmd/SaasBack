package com.saas.auth.infrastructure.security;

import com.saas.common.security.IUserPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Principal autenticado en auth-service. Implementa:
 *   - {@link UserDetails}      para Spring Security.
 *   - {@link IUserPrincipal}   para que {@code AuditorAwareImpl} (saas-common)
 *                              resuelva el AuditUser sin acoplamiento a auth.
 */
@Getter
public class AppUserPrincipal implements UserDetails, IUserPrincipal {

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<String> roles;

    public AppUserPrincipal(UUID userId, String username, String passwordHash,
                             boolean enabled, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());
    }

    @Override public String  getPassword()              { return passwordHash; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return enabled; }
}
