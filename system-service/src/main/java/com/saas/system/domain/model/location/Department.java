package com.saas.system.domain.model.location;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Department extends BaseDomain implements ICodeable {

    private String code;
    private String name;
    private UUID countryId;
}
