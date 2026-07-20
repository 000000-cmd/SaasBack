package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.util.UUID;

/** Especialidad per-business (Barberia, Estilismo, Manicure...). Agrupa
 *  servicios por disciplina y clasifica al empleado. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Specialty extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private String name;
    private Integer displayOrder;
}
