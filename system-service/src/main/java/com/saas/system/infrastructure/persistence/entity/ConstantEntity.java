package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Constante de configuracion global. Value se almacena siempre como STRING; el
 * consumidor decide como interpretarlo (numero, boolean, json...).
 *
 * Ejemplo: Code=MAYORIA_EDAD, Value="18".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "constant")
public class ConstantEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Value", nullable = false, length = 1000)
    private String value;

    @Column(name = "Description", length = 500)
    private String description;
}
