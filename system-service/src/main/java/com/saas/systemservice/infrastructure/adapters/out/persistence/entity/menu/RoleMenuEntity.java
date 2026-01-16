package com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu;

import com.saas.saascommon.persistence.BaseEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists.RoleEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "rolemenu", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"RoleId", "MenuId"})
})
@EqualsAndHashCode(callSuper = true, exclude = {"role", "menu"})
@ToString(exclude = {"role", "menu"})
public class RoleMenuEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", unique = true, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleId", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MenuId", nullable = false)
    private MenuEntity menu;
}