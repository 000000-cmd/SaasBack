package com.saas.system.domain.model;

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
public class Constant extends BaseDomain implements ICodeable {

    private String code;
    private String name;
    private String value;
    private String description;

    /** Helpers tipados sobre el value (todo se guarda como STRING). */
    public Integer getValueAsInt() {
        return value == null ? null : Integer.valueOf(value);
    }

    public Boolean getValueAsBoolean() {
        return value == null ? null : Boolean.valueOf(value);
    }
}
