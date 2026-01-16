package com.saas.systemservice.domain.model;

import com.saas.saascommon.model.BaseDomain;
import com.saas.saascommon.model.IBusinessEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Constant extends BaseDomain implements IBusinessEntity<String> {
    private String id;
    private String code;
    private String value;
    private String description;
}
