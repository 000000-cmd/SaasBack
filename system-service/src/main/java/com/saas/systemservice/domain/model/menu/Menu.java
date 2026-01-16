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
public class Menu extends BaseDomain implements IBusinessEntity<String> {

    private String id;
    private String code;
    private String label;
    private String routerLink;
    private String icon;
    private Integer order;
    private String parentId;
}
