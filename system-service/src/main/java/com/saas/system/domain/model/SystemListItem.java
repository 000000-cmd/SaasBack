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
 * Item de una {@link SystemList}. {@code Code} es unico solo dentro de su lista,
 * por eso este dominio NO implementa {@link com.saas.common.model.ICodeable}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SystemListItem extends BaseDomain {

    private UUID listId;
    private String code;
    private String name;
    private String value;
    private Integer displayOrder;
}
