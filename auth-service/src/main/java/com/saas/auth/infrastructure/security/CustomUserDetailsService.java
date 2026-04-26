package com.saas.auth.infrastructure.security;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserRepositoryPort userRepository;
    private final IUserUseCase userUseCase;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail));

        // Resolver roleCodes (via Feign en Phase 7; vacio en Phase 5).
        User withRoles = userUseCase.loadWithRoles(user.getId());

        return new AppUserPrincipal(
                withRoles.getId(),
                withRoles.getUsername(),
                withRoles.getPasswordHash(),
                Boolean.TRUE.equals(withRoles.getEnabled()),
                withRoles.getRoleCodes()
        );
    }
}
