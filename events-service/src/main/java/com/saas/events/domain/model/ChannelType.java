package com.saas.events.domain.model;

/**
 * Canales de envio. El codigo coincide con el del catalogo notification_type
 * de saas_db: el catalogo decide etiqueta y orden en la interfaz, este enum
 * decide que sabe hacer el notificador.
 */
public enum ChannelType {
    EMAIL, SMS, WHATSAPP,
    /**
     * Notificacion del sistema operativo en el telefono, via FCM. Es UN solo
     * canal: no hay "push de web" y "push de movil" por separado. Lo que se ve
     * en la web es la BANDEJA (notification_inbox), que no es un canal sino la
     * superficie de lectura y se escribe en todo envio, salga por donde salga.
     * Si algun dia se quiere Web Push del navegador, entra como otro transporte
     * de este mismo canal segun el Platform del dispositivo suscrito.
     */
    PUSH;

    /** Devuelve null si el codigo no corresponde a ningun canal conocido. */
    public static ChannelType from(String code) {
        if (code == null) return null;
        for (ChannelType t : values()) {
            if (t.name().equalsIgnoreCase(code.trim())) return t;
        }
        return null;
    }
}
