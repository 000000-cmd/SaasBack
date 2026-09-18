package com.saas.events.application.service;

import com.saas.common.security.IUserPrincipal;
import com.saas.events.infrastructure.client.ThirdPartyInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Del usuario en sesión al TERCERO cuya bandeja hay que leer.
 *
 * La bandeja se guarda por tercero porque no todo destinatario tiene cuenta (a
 * un cliente se le avisa de su cita y puede no entrar nunca al sistema). Pero
 * quien abre la campana se identifica con su usuario. Este puente estaba
 * escrito dentro del controller de la bandeja; al aparecer el flujo en vivo
 * habría hecho falta una segunda copia, así que se saca aquí.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxOwnerResolver {

    private final ThirdPartyInternalClient thirdParty;

    /**
     * Tercero del usuario en sesión, o {@code null} si su cuenta no tiene uno —
     * que es el caso del administrador del sistema, y por eso su bandeja sale
     * vacía en vez de reventar.
     */
    public UUID of(IUserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) return null;
        try {
            ThirdPartyInternalClient.ThirdPartyRef ref = thirdParty.byUser(principal.getUserId());
            return ref == null ? null : ref.id();
        } catch (Exception ex) {
            log.debug("El usuario {} no tiene tercero asociado: {}",
                    principal.getUserId(), ex.getMessage());
            return null;
        }
    }
}
