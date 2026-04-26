package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "menu")
public class MenuEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 50)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Icon", length = 60)
    private String icon;

    @Column(name = "Route", length = 200)
    private String route;

    /** Auto-FK para jerarquia padre/hijo. Null = seccion principal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentId", foreignKey = @ForeignKey(name = "fk_menu_parent"))
    private MenuEntity parent;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder;
}
