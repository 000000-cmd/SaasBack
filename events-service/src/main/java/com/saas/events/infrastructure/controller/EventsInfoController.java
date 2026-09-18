package com.saas.events.infrastructure.controller;

import com.saas.common.controller.BaseInfoController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone /api/info y /api/version (heredados de saas-common) bajo los DOS
 * prefijos que sirve este servicio. Es necesario porque al fusionar auditoria
 * y notificaciones se elimino el context-path: sin estos prefijos explicitos
 * el endpoint quedaria en /api/info, que ninguna ruta del gateway enruta, y
 * el panel de estado del sistema mostraria el servicio como caido.
 */
@RestController
@RequestMapping({"/audit", "/notification"})
public class EventsInfoController extends BaseInfoController {
}
