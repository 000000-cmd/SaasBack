package com.saas.system.application.dto.response.flow;

import java.util.List;
import java.util.UUID;

/**
 * Lo que ve la pantalla de configuracion: el flujo ENTERO, sin filtrar por
 * canal ni por permiso.
 *
 * <p>Aqui se ensena todo incluso lo desactivado: quien configura tiene que
 * poder ver lo que apago para volver a encenderlo.</p>
 */
public record FlowAdminView(
        UUID id, String code, String name, String description,
        boolean isSystem, boolean enabled, List<Section> sections) {

    public record Section(
            UUID id, String code, String name, String description,
            int displayOrder, String kind, String channels,
            boolean isSystem, boolean enabled,
            List<Field> fields, List<Control> controls) {}

    public record Field(
            UUID id, String code, String label, String placeholder, String helpText,
            String dataType, String maskCode, String sourceKey,
            boolean isRequired, String width, int displayOrder,
            boolean isSystem, boolean enabled) {}

    public record Control(
            UUID id, String code, String label, String action, String variant,
            String permissionCode, String channels, int displayOrder,
            boolean isSystem, boolean enabled) {}

    /** Fila del listado. Sin el arbol: la lista no necesita cargarlo. */
    public record Summary(UUID id, String code, String name, String description,
                          boolean isSystem, boolean enabled, int sectionCount) {}
}
