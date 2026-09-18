package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationParameter extends BaseDomain implements ICodeable {
    private String code;
    private String name;
    private String description;
}
