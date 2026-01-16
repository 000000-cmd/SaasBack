package com.saas.systemservice.domain.model.lists;

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
public class Role extends BaseDomain implements IBusinessEntity<String> {

    private String id;
    private String code;
    private String name;
    private String description;
}