package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "sys_systemlistdefinition")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemListDefinitionEntity extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "DisplayName", nullable = false)
    private String displayName;

    @Column(name = "PhysicalTableName", nullable = false, unique = true)
    private String physicalTableName;
}