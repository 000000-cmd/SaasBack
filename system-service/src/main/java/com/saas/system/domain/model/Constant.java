package com.saas.system.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * Helpers tipados sobre el value (todo se guarda como STRING). Son de uso
     * INTERNO, no propiedades de API: {@code @JsonIgnore} evita que Jackson los
     * serialice (una constante no numérica como VERAPP="1.0.0" rompía la
     * serialización con NumberFormatException). Toleran valores no numéricos
     * devolviendo null en vez de lanzar.
     */
    @JsonIgnore
    public Integer getValueAsInt() {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @JsonIgnore
    public Boolean getValueAsBoolean() {
        return value == null ? null : Boolean.valueOf(value.trim());
    }
}
