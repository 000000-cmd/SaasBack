package com.saas.auth.application.dto.request;

/**
 * De que aparato viene el intento de entrar.
 *
 * <p>{@code deviceId} es el unico campo que importa de verdad: es el serial
 * interno que genera el propio telefono y que la app guarda en el llavero del
 * sistema (Keystore / Keychain), NO en sus datos. Sobrevive a desinstalar la
 * app, borrar sus datos o limpiar la cache. Lo demas es para poder decirle a la
 * persona de que aparato se trata sin que tenga que adivinarlo.</p>
 *
 * <p>Todo es opcional: la web no manda nada de esto, y ahi no hay regla de un
 * dispositivo por cuenta — un dueno abre el panel en el portatil y en el
 * escritorio, y eso es normal. La regla es del APK.</p>
 */
public record DeviceInfo(
        String deviceId,
        String deviceName,
        String platform,
        String appVersion
) {
    public boolean isUsable() {
        return deviceId != null && !deviceId.isBlank();
    }

    public String platformOrDefault() {
        return platform == null || platform.isBlank() ? "ANDROID" : platform.trim().toUpperCase();
    }
}
