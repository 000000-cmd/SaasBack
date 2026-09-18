package com.saas.business.domain.model;

/**
 * Desde donde se creo o se movio una cita.
 *
 * <p>No es estadistica: de aqui depende que reglas aplican. La web publica no
 * puede saltarse la antelacion minima; el panel del negocio si, porque detras
 * hay alguien que responde por esa cita. Lo que NADIE puede saltarse es la
 * comprobacion de solapamiento — son dos validaciones distintas, y confundirlas
 * en una bandera de "es admin" es como se acaban agendando dos personas a la
 * misma hora.</p>
 */
public enum AppointmentChannel {

    /** El cliente, desde el subdominio del negocio. Sin cuenta. */
    WEB_PUBLICA,

    /** El dueño o quien tenga permiso, desde el panel. */
    PANEL_DUENO,

    /** El empleado, desde su app. */
    APK_EMPLEADO,

    /** El formulario conversado por WhatsApp. */
    WHATSAPP,

    /** El cliente, llegando desde el directorio publico. */
    MARKETPLACE;

    /**
     * Detras hay alguien del negocio, con sesion y con permisos.
     *
     * <p>Solo estos pueden registrar una cita retroactiva o saltarse la
     * antelacion minima.</p>
     */
    public boolean isStaff() {
        return this == PANEL_DUENO || this == APK_EMPLEADO;
    }
}
