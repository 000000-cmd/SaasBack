package com.saas.auth.infrastructure.controller;

import com.saas.common.controller.BaseInfoController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para información del Auth Service.
 */
@RestController
@RequestMapping
public class AuthInfoController extends BaseInfoController {
    // Hereda /api/info y /api/version del BaseInfoController
}