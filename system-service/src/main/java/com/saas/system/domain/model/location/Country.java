package com.saas.system.domain.model.location;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Country extends BaseDomain implements ICodeable {

    private String code;
    private String name;
    private String officialName;
    private String isoCode3;
    private String numericCode;
    private String phoneCode;
    private String currencyCode;
    private String currencySymbol;
    private String continent;
}
