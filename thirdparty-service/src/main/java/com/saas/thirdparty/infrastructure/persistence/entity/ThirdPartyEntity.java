package com.saas.thirdparty.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA del tercero (persona natural). Columnas PascalCase, tabla singular.
 */
@Getter
@Setter
@Entity
@Table(
        name = "third_party",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_third_party_document",
                columnNames = {"DocumentTypeId", "DocumentNumber"})
)
@SQLRestriction("Visible = 1")
public class ThirdPartyEntity extends BaseEntity {

    // Nullable desde V3: el alta minima de empleado crea un tercero "shell"
    // (solo userId+businessId); el documento llega despues desde el APK.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "DocumentTypeId", length = 36)
    private UUID documentTypeId;

    @Column(name = "DocumentNumber", length = 40)
    private String documentNumber;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "UserId", length = 36)
    private UUID userId;

    @Column(name = "FirstName", length = 80)
    private String firstName;

    @Column(name = "SecondName", length = 80)
    private String secondName;

    @Column(name = "FirstLastName", length = 80)
    private String firstLastName;

    @Column(name = "SecondLastName", length = 80)
    private String secondLastName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "GenderId", length = 36)
    private UUID genderId;

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "PhotoUrl", length = 500)
    private String photoUrl;

    @Column(name = "BiometricEnabled", nullable = false)
    private Boolean biometricEnabled = false;
}
