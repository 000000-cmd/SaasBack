package com.saas.auth.application.service;

import com.saas.auth.application.dto.request.DeviceInfo;
import com.saas.auth.application.dto.response.DeviceConflictResponse.Conflict;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.model.UserDeviceLink;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.domain.port.out.IUserDeviceLinkRepositoryPort;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Las dos reglas de "una cuenta, un aparato".
 *
 * <ol>
 *   <li>Una cuenta esta abierta en UN aparato a la vez. Si intenta entrar desde
 *       otro, se avisa y la persona decide.</li>
 *   <li>Un aparato tiene UNA cuenta abierta. Si entra otra persona en el mismo
 *       telefono, se avisa igual.</li>
 * </ol>
 *
 * <p>Ninguna bloquea, y es a proposito. Bloquear dejaria fuera a quien cambia de
 * telefono, a quien lo formatea o a quien lo presta un rato — casos normales
 * todos. Lo que hace falta no es impedir el segundo acceso, es que nadie se
 * entere tarde de que su cuenta esta abierta en otro sitio.</p>
 *
 * <p>Desvincular no es solo marcar la fila: se revocan tambien los refresh
 * tokens de esa cuenta. Sin eso, el aparato "desvinculado" seguiria renovando su
 * sesion tranquilamente durante una semana y la desvinculacion seria un adorno.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLinkService {

    private final IUserDeviceLinkRepositoryPort links;
    private final IRefreshTokenRepositoryPort refreshTokens;
    private final IUserRepositoryPort users;

    /**
     * Que hay que avisar antes de dejar entrar. Lista vacia = adelante.
     *
     * <p>Es de solo lectura a proposito: se consulta ANTES de emitir tokens, y
     * si la persona cancela no debe haber quedado rastro de nada.</p>
     */
    @Transactional(readOnly = true)
    public List<Conflict> conflicts(UUID userId, DeviceInfo device) {
        if (device == null || !device.isUsable()) return List.of();
        List<Conflict> out = new ArrayList<>();

        // Caso 1: la misma cuenta, abierta en OTRO aparato.
        for (UserDeviceLink l : links.activeOfUser(userId)) {
            if (device.deviceId().equals(l.getDeviceId())) continue;
            out.add(new Conflict(l.getId(), Conflict.OTHER_DEVICE,
                    nombre(l), l.getPlatform(), null, l.getLastSeenAt()));
        }

        // Caso 2: este aparato, con OTRA cuenta abierta.
        for (UserDeviceLink l : links.activeOfDevice(device.deviceId())) {
            if (userId.equals(l.getUserId())) continue;
            out.add(new Conflict(l.getId(), Conflict.OTHER_ACCOUNT,
                    nombre(l), l.getPlatform(), oculta(l.getUserId()), l.getLastSeenAt()));
        }
        return out;
    }

    /**
     * Deja el vinculo de este par usuario+aparato como el unico activo.
     *
     * @param unlinkOthers cuando la persona ya confirmo el aviso. Sin esto, solo
     *        se registra o refresca el vinculo propio y no se toca nada ajeno.
     */
    @Transactional
    public void bind(User user, DeviceInfo device, boolean unlinkOthers) {
        if (device == null || !device.isUsable()) return;

        if (unlinkOthers) {
            for (Conflict c : conflicts(user.getId(), device)) {
                links.revoke(c.linkId(), user.getId());
                // La sesion del aparato desvinculado tiene que morir de verdad.
                if (Conflict.OTHER_DEVICE.equals(c.kind())) {
                    refreshTokens.revokeAllByUserId(user.getId());
                } else {
                    links.find(otroUsuario(c), device.deviceId())
                         .ifPresent(l -> refreshTokens.revokeAllByUserId(l.getUserId()));
                }
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        UserDeviceLink existente = links.find(user.getId(), device.deviceId()).orElse(null);
        if (existente == null) {
            links.save(UserDeviceLink.builder()
                    .userId(user.getId())
                    .deviceId(device.deviceId())
                    .deviceName(device.deviceName())
                    .platform(device.platformOrDefault())
                    .appVersion(device.appVersion())
                    .lastSeenAt(ahora)
                    .build());
            log.info("Aparato vinculado: userId={} device={}", user.getId(), device.deviceName());
            return;
        }

        // Reactivar es lo normal, no la excepcion: quien vuelve a su telefono de
        // siempre despues de haber entrado en otro cae justo aqui.
        existente.setRevokedAt(null);
        existente.setRevokedBy(null);
        existente.setDeviceName(device.deviceName());
        existente.setPlatform(device.platformOrDefault());
        existente.setAppVersion(device.appVersion());
        existente.setLastSeenAt(ahora);
        links.update(existente);
    }

    /** Los aparatos activos de una cuenta, para la pantalla "mis dispositivos". */
    @Transactional(readOnly = true)
    public List<UserDeviceLink> active(UUID userId) {
        return links.activeOfUser(userId);
    }

    /** Desvincular a mano desde el perfil. */
    @Transactional
    public boolean revoke(UUID linkId, UUID actor) {
        return links.revoke(linkId, actor) > 0;
    }

    // -----------------------------------------------------------------

    private UUID otroUsuario(Conflict c) {
        // El id del vinculo basta para localizar la fila; se resuelve su dueno.
        return links.findById(c.linkId()).map(UserDeviceLink::getUserId).orElse(null);
    }

    private static String nombre(UserDeviceLink l) {
        return l.getDeviceName() == null || l.getDeviceName().isBlank()
                ? "Otro dispositivo" : l.getDeviceName();
    }

    /**
     * "andres" -> "an***s". Quien esta intentando entrar tiene derecho a saber
     * que el telefono tiene otra cuenta abierta, no a saber cual.
     */
    private String oculta(UUID userId) {
        String u = users.findById(userId).map(User::getUsername).orElse(null);
        if (u == null || u.length() <= 2) return "otra cuenta";
        if (u.length() <= 4) return u.charAt(0) + "***";
        return u.substring(0, 2) + "***" + u.charAt(u.length() - 1);
    }
}
