package com.saas.systemservice.infrastructure.adapters.out.persistence.entity;

import com.saas.saascommon.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "systemlistdefinition")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemListDefinitionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "DisplayName", nullable = false)
    private String displayName;

    @Column(name = "PhysicalTableName", nullable = false, unique = true)
    private String physicalTableName;
}