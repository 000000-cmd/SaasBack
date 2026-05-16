package com.saas.system.infrastructure.persistence.entity.location;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "country")
@SQLRestriction("Visible = 1")
public class CountryEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 16)
    private String code;

    @Column(name = "Name", nullable = false, length = 160)
    private String name;

    @Column(name = "OfficialName", length = 255)
    private String officialName;

    @Column(name = "IsoCode3", length = 16)
    private String isoCode3;

    @Column(name = "NumericCode", length = 16)
    private String numericCode;

    @Column(name = "PhoneCode", length = 16)
    private String phoneCode;

    @Column(name = "CurrencyCode", length = 16)
    private String currencyCode;

    @Column(name = "CurrencySymbol", length = 16)
    private String currencySymbol;

    @Column(name = "Continent", length = 64)
    private String continent;
}
