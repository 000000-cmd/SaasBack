package com.saas.system.domain.model.flow;

/**
 * Donde se pinta un flujo.
 *
 * <p>No es adorno: WhatsApp no puede dibujar un calendario, asi que la seccion
 * de fecha se declara para los otros tres canales y el bot la resuelve con una
 * lista de opciones. Sin este campo, el bot intentaria renderizar un selector
 * de fecha dentro de un mensaje de texto.</p>
 */
public enum FlowChannel {
    WEB, PANEL, APK, WHATSAPP;

    public static FlowChannel of(String raw) {
        if (raw == null || raw.isBlank()) return WEB;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return WEB;
        }
    }

    /** El canal esta en un CSV como "WEB,PANEL,APK". Vacio = vale para todos. */
    public boolean inCsv(String csv) {
        if (csv == null || csv.isBlank()) return true;
        for (String parte : csv.split(",")) {
            if (name().equalsIgnoreCase(parte.trim())) return true;
        }
        return false;
    }
}
