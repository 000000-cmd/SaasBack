package com.saas.auth.application.exception;

import com.saas.auth.application.dto.response.DeviceConflictResponse.Conflict;
import lombok.Getter;

import java.util.List;

/**
 * La contrasena era correcta, pero hay una sesion abierta en otro sitio.
 *
 * <p>No es un fallo de credenciales y no debe parecerlo: se responde 409 con el
 * detalle de que aparato es y desde cuando, para que la app pueda preguntar
 * "¿desvinculamos el otro?" en vez de decir "no se pudo entrar".</p>
 */
@Getter
public class DeviceConflictException extends RuntimeException {

    private final transient List<Conflict> conflicts;

    public DeviceConflictException(String message, List<Conflict> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }
}
