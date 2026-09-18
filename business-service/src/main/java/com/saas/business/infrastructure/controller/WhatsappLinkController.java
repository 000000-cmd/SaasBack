package com.saas.business.infrastructure.controller;

import com.saas.business.application.service.WhatsappLinkService;
import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Conectar el WhatsApp del negocio, desde el panel del dueno.
 *
 * <h3>El negocio sale del token, nunca del cuerpo</h3>
 * Si viniera por parametro, un dueno podria conectar —o desconectar— el
 * WhatsApp de otro negocio escribiendo su id. Aqui el negocio es SIEMPRE el de
 * la sesion.
 */
@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsappLinkController {

    private final WhatsappLinkService link;

    /**
     * @param wabaId el identificador de la CUENTA de WhatsApp Business. Con el
     *        se da de alta el webhook por API y el dueno no vuelve a Meta.
     *        Opcional: sin el, la conexion funciona igual y el alta del webhook
     *        se hace a mano.
     * @param appSecret el secreto de la app de Meta del negocio. Opcional al
     *        conectar, pero SIN EL no se puede comprobar la firma de lo que
     *        Meta entrega: se podra enviar, y no recibir.
     */
    public record ConnectRequest(
            @NotBlank @Size(max = 40) String phoneNumberId,
            @Size(max = 40) String wabaId,
            @NotBlank @Size(max = 1000) String accessToken,
            @Size(max = 200) String appSecret) {}

    public record TestRequest(@NotBlank @Size(max = 30) String phone) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappLinkService.Estado>> estado(
            @AuthenticationPrincipal IUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(link.estado(negocioDe(principal))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappLinkService.Estado>> conectar(
            @AuthenticationPrincipal IUserPrincipal principal,
            @Valid @RequestBody ConnectRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                link.conectar(negocioDe(principal), req.phoneNumberId(), req.wabaId(),
                        req.accessToken(), req.appSecret()),
                "WhatsApp conectado"));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappLinkService.Estado>> desconectar(
            @AuthenticationPrincipal IUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                link.desconectar(negocioDe(principal)), "WhatsApp desconectado"));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> probar(
            @AuthenticationPrincipal IUserPrincipal principal,
            @Valid @RequestBody TestRequest req) {
        link.probar(negocioDe(principal), req.phone());
        return ResponseEntity.ok(ApiResponse.success(null, "Mensaje de prueba enviado"));
    }

    private UUID negocioDe(IUserPrincipal principal) {
        UUID id = principal == null ? null : principal.getBusinessId();
        if (id == null) throw new AccessDeniedException("Tu cuenta no tiene un negocio");
        return id;
    }
}
