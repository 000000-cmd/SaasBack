package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "app_version")
@SQLRestriction("Visible = 1")
public class AppVersionEntity extends BaseEntity {

    @Column(name = "Version", length = 20, nullable = false, unique = true)
    private String version;

    @Column(name = "VersionCode", nullable = false, unique = true)
    private Integer versionCode;

    @Column(name = "FileName", length = 160, nullable = false)
    private String fileName;

    @Column(name = "Checksum", length = 64, nullable = false)
    private String checksum;

    @Column(name = "SizeBytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "IsCurrent", nullable = false)
    private Boolean isCurrent = false;
}
