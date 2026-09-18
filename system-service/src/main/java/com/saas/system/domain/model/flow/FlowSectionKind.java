package com.saas.system.domain.model.flow;

/**
 * Que clase de paso es una seccion.
 *
 * <ul>
 *   <li>{@code FORM} pide datos: los que define {@code flow_field}.</li>
 *   <li>{@code SELECTION} se elige de una lista que calcula el servidor
 *       (servicios, profesionales, horas libres).</li>
 *   <li>{@code REVIEW} resume antes de confirmar.</li>
 *   <li>{@code INFO} solo informa (el codigo publico al final).</li>
 * </ul>
 *
 * <p>Solo {@code FORM} lee campos. El resto los ignora aunque los tenga.</p>
 */
public enum FlowSectionKind {
    FORM, SELECTION, REVIEW, INFO;

    public static FlowSectionKind of(String raw) {
        if (raw == null || raw.isBlank()) return FORM;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FORM;
        }
    }
}
