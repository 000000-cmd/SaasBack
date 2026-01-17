package com.saas.system.infrastructure.controller;

import com.saas.common.controller.BaseInfoController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para información del System Service.
 */
@RestController
@RequestMapping
public class SystemInfoController extends BaseInfoController {
    // Hereda /api/info y /api/version del BaseInfoController
}