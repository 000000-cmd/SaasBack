package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Un paso del tour guiado. Resuelve a que apunta en cascada:
 * {@code menuId} -> ancla derivada {@code nav-{menuCode}}; si no,
 * {@code anchor} literal; si no, es una diapositiva a pantalla completa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TourStep extends BaseDomain {

    private UUID menuId;
    /** Codigo del menu asociado. DERIVADO: se lee, nunca se escribe. */
    private String menuCode;
    private String anchor;
    private String title;
    private String body;
    private String icon;
    private Integer displayOrder;
}
