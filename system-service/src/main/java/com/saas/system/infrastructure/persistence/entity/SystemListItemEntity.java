package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
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

/**
 * Item dentro de una {@link SystemListEntity}.
 * El {@code Code} es unico por lista (no global).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "system_list_item",
        uniqueConstraints = @UniqueConstraint(name = "uq_system_list_item_code", columnNames = {"ListId", "Code"})
)
public class SystemListItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ListId", nullable = false, foreignKey = @ForeignKey(name = "fk_system_list_item_list"))
    private SystemListEntity list;

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Value", length = 500)
    private String value;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder;
}
