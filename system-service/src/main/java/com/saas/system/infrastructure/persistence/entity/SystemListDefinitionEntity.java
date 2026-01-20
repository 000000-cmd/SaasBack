package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sys_systemlistdefinition")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemListDefinitionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", columnDefinition = "VARCHAR(36)")
    private Long id;

    @Column(name = "DisplayName", nullable = false)
    private String displayName;

    @Column(name = "PhysicalTableName", nullable = false, unique = true)
    private String physicalTableName;
}