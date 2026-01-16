package com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists;

import com.saas.saascommon.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "role")
@EqualsAndHashCode(callSuper = true)
public class RoleEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "Code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Description")
    private String description;
}