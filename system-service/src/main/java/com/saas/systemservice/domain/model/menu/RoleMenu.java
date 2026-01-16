package com.saas.systemservice.domain.model.menu;

import com.saas.saascommon.model.BaseDomain;
import com.saas.saascommon.model.IBusinessEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleMenu extends BaseDomain {

    private String id;

    private String roleId;
    private String menuId;

    private String roleCode;
    private String menuCode;
}
