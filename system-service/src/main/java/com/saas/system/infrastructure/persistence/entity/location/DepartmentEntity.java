package com.saas.system.infrastructure.persistence.entity.location;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "department")
@SQLRestriction("Visible = 1")
public class DepartmentEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 32)
    private String code;

    @Column(name = "Name", nullable = false, length = 160)
    private String name;

    /**
     * columnDefinition fuerza CHAR(36) porque Hibernate 6 por defecto mapea
     * {@link UUID} a BINARY(16), lo que rompe el schema-validation contra
     * el DDL de V7 (CHAR(36)). Mismo patron para todas las FK del location.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "CountryId", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private UUID countryId;
}
