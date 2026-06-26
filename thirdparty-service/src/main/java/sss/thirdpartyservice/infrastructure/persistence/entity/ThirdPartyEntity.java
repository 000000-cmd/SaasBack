package sss.thirdpartyservice.infrastructure.persistence.entity;


import com.saas.common.persistence.BaseEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sss.thirdpartyservice.domain.model.ThirdPartyType;

import java.util.UUID;

@Entity
@Table(
        name = "third_parties",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "document_type_id",
                                "document_number"
                        }
                )
        }
)
public class ThirdPartyEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThirdPartyType type;
    ///  AuthService
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "document_type_id", length = 36, nullable = false)
    private UUID documentTypeId;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;
    ///  AuthService
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "second_name")
    private String secondName;

    @Column(name = "first_last_name")
    private String firstLastName;

    @Column(name = "second_last_name")
    private String secondLastName;

    private String email;

    private String phone;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(nullable = false)
    private boolean active;
}