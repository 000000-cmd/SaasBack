package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "gender")
@SQLRestriction("Visible = 1")
public class GenderEntity extends BaseCatalogEntity {
}
