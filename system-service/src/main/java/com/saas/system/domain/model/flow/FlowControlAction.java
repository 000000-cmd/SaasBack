package com.saas.system.domain.model.flow;

/**
 * Que hace un boton.
 *
 * <p>El texto y el color son configuracion; el COMPORTAMIENTO no. El front mapea
 * cada accion a su manejador, asi que inventarse una accion nueva desde la
 * pantalla de configuracion no crearia funcionalidad: crearia un boton que no
 * hace nada. Por eso es un enumerado y no texto libre.</p>
 */
public enum FlowControlAction {
    NEXT, BACK, SAVE, CONFIRM, CANCEL, EDIT, DELETE, CUSTOM;

    public static FlowControlAction of(String raw) {
        if (raw == null || raw.isBlank()) return CUSTOM;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CUSTOM;
        }
    }
}
