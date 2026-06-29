package com.saas.system.infrastructure.persistence.entity.business;

import com.saas.common.persistence.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "address_type")
@SQLRestriction("Visible = 1")
public class AddressTypeEntity extends BaseCatalogEntity {
}
