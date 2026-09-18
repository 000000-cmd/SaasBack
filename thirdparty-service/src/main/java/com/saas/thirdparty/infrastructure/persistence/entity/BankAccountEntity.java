package com.saas.thirdparty.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import com.saas.thirdparty.domain.model.AccountKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "bank_account")
@SQLRestriction("Visible = 1")
public class BankAccountEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "AccountKind", length = 16, nullable = false)
    private AccountKind accountKind;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "BankId", length = 36)
    private UUID bankId;

    @Column(name = "AccountType", length = 16)
    private String accountType;

    @Column(name = "AccountNumber", length = 40)
    private String accountNumber;

    @Column(name = "BrevKey", length = 120)
    private String brevKey;

    @Column(name = "Alias", length = 60)
    private String alias;

    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary = Boolean.FALSE;

    // PrimaryOwner es una columna GENERADA por MySQL (vale ThirdPartyId cuando
    // la cuenta es principal y NULL cuando no) y es la que lleva el UNIQUE que
    // impide dos principales. No se mapea a proposito: escribirla desde aqui
    // seria un error de MySQL, y leerla no aporta nada que IsPrimary no diga.
}
