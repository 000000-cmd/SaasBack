package com.saas.saascommon.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseDomain {
    private boolean enabled;
    private String auditUser;
    private LocalDateTime auditDate;
}