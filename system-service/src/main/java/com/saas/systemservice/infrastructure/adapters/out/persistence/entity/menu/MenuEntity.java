package com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu;

import com.saas.saascommon.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "menu")
@EqualsAndHashCode(callSuper = true)
public class MenuEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "Code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "Label", nullable = false, length = 50)
    private String label;

    @Column(name = "RouterLink", length = 200)
    private String routerLink;

    @Column(name = "Icon", length = 50)
    private String icon;

    @Column(name = "DisplayOrder")
    private Integer order;

    @Column(name = "ParentId")
    private String parentId;
}
