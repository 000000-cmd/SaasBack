package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseDomain implements ICodeable {
    private String code;
    private String name;
    private String description;

    /**
     * Solo las marcadas aparecen en "Lanzamiento global". Arranca en false: una
     * notificacion se vuelve lanzable a toda la base cuando alguien lo decide.
     */
    private Boolean isGlobal;
}
