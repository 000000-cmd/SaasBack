package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "business")
@SQLRestriction("Visible = 1")
public class BusinessEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "BusinessTypeId", length = 36, nullable = false)
    private UUID businessTypeId;

    @Column(name = "Name", length = 160, nullable = false)
    private String name;

    @Column(name = "LegalName", length = 200)
    private String legalName;

    @Column(name = "TradeName", length = 160)
    private String tradeName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "DocumentTypeId", length = 36)
    private UUID documentTypeId;

    @Column(name = "DocumentNumber", length = 40)
    private String documentNumber;

    @Column(name = "LogoUrl", length = 500)
    private String logoUrl;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "StatusId", length = 36)
    private UUID statusId;

    @Column(name = "PrimaryColor", length = 20)
    private String primaryColor;

    @Column(name = "SecondaryColor", length = 20)
    private String secondaryColor;
}
