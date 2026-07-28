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
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tour_step")
@SQLRestriction("Visible = 1")
public class TourStepEntity extends BaseEntity {

    /** EAGER a proposito: el codigo del menu viaja en TODAS las respuestas. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MenuId", foreignKey = @ForeignKey(name = "fk_tour_step_menu"))
    private MenuEntity menu;

    @Column(name = "Anchor", length = 60)
    private String anchor;

    @Column(name = "Title", nullable = false, length = 160)
    private String title;

    @Column(name = "Body", nullable = false, length = 500)
    private String body;

    @Column(name = "Icon", length = 60)
    private String icon;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder;
}
