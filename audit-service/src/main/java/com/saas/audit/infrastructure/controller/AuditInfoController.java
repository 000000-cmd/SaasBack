package com.saas.audit.infrastructure.controller;

import com.saas.common.controller.BaseInfoController;
import org.springframework.web.bind.annotation.RestController;

/** Expone /api/info y /api/version del audit-service (heredado de saas-common). */
@RestController
public class AuditInfoController extends BaseInfoController {
}
