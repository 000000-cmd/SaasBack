package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "menu_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_menu_role", columnNames = {"MenuId", "RoleId"})
)
public class MenuRoleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MenuId", nullable = false, foreignKey = @ForeignKey(name = "fk_menu_role_menu"))
    private MenuEntity menu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoleId", nullable = false, foreignKey = @ForeignKey(name = "fk_menu_role_role"))
    private RoleEntity role;
}
