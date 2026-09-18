package com.saas.system.application.dto.response.flow;

import java.util.List;

/**
 * El flujo LISTO PARA CONSUMIR: solo lo activo, solo lo de este canal, y solo
 * los botones que quien pregunta tiene permiso de usar.
 *
 * <p>El filtrado va en el servidor y no en cada cliente. Si cada front
 * decidiera por su cuenta que botones esconder, el APK y el bot de WhatsApp
 * tendrian que reimplementar la misma regla, y el que se equivoque ensena un
 * boton que no deberia existir.</p>
 */
public record ResolvedFlowView(
        String code, String name, String description, String channel,
        List<Section> sections) {

    public record Section(
            String code, String name, String description,
            int step, String kind, List<Field> fields, List<Control> controls) {}

    public record Field(
            String code, String label, String placeholder, String helpText,
            String dataType, String mask, String sourceKey,
            boolean required, String width) {}

    public record Control(String code, String label, String action, String variant) {}
}
