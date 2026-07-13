package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "business_landing")
@SQLRestriction("Visible = 1")
public class BusinessLandingEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "BusinessId", length = 36, nullable = false, unique = true)
    private UUID businessId;

    @Column(name = "Tagline", length = 160)
    private String tagline;

    @Column(name = "About", columnDefinition = "TEXT")
    private String about;

    @Column(name = "Phone", length = 40)
    private String phone;

    @Column(name = "Whatsapp", length = 40)
    private String whatsapp;

    @Column(name = "ContactEmail", length = 120)
    private String contactEmail;

    @Column(name = "Instagram", length = 160)
    private String instagram;

    @Column(name = "Facebook", length = 160)
    private String facebook;

    @Column(name = "HeroImageUrl", length = 500)
    private String heroImageUrl;

    @Column(name = "GalleryJson", columnDefinition = "JSON")
    private String galleryJson;

    @Column(name = "ScheduleText", length = 400)
    private String scheduleText;

    @Column(name = "Published", nullable = false)
    private Boolean published;
}
