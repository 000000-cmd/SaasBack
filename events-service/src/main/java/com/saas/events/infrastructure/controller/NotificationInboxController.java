package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.dto.PagedResponse;
import com.saas.common.security.IUserPrincipal;
import com.saas.events.domain.model.NotificationInbox;
import com.saas.events.application.service.InboxOwnerResolver;
import com.saas.events.domain.port.out.INotificationInboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La BANDEJA de un destinatario. La leen la campana de la web y la lista del
 * movil: es la misma tabla, escrita en todo envio, de modo que no hay dos
 * historiales que puedan discrepar.
 *
 * Hay dos familias de endpoints y no es duplicacion:
 *
 *  - Los que reciben {@code thirdPartyId} sirven para consultar la bandeja de
 *    CUALQUIERA: el destinatario es un tercero (cliente, empleado o dueno) y no
 *    todos tienen cuenta de usuario.
 *  - Los {@code /me} son los que consume la campana, y resuelven el tercero a
 *    partir del usuario en sesion para que el front no tenga que conocer la
 *    existencia de los terceros.
 */
@Slf4j
@RestController
@RequestMapping("/notification/inbox")
@RequiredArgsConstructor
public class NotificationInboxController {

    private final INotificationInboxRepositoryPort inbox;
    private final InboxOwnerResolver owners;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationInbox>>> list(
            @RequestParam UUID thirdPartyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(inbox.findForOwner(thirdPartyId, page, size)));
    }

    /** Lo que pinta el globito. Es la llamada mas frecuente de todo el subsistema. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@RequestParam UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", inbox.countUnread(thirdPartyId))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationInbox>> markRead(@PathVariable UUID id) {
        NotificationInbox item = inbox.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        // Idempotente: marcar dos veces no mueve la fecha original de lectura.
        if (item.getReadAt() == null) {
            item.setReadAt(LocalDateTime.now());
            item = inbox.save(item);
        }
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(@RequestParam UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("updated", inbox.markAllRead(thirdPartyId))));
    }

    // =================================================================
    // Variantes /me — las que consume la campana del panel.
    //
    // La bandeja se guarda por TERCERO porque no todo destinatario tiene cuenta
    // (un cliente al que se le avisa de una cita, por ejemplo). Pero quien abre
    // la campana se identifica con su USUARIO. Resolver el puente aqui evita que
    // el front tenga que conocer la existencia de los terceros.
    // =================================================================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationInbox>>> mine(
            @AuthenticationPrincipal IUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID owner = owners.of(principal);
        if (owner == null) {
            return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(List.of(), page, size, 0)));
        }
        return ResponseEntity.ok(ApiResponse.success(inbox.findForOwner(owner, page, size)));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> myUnreadCount(
            @AuthenticationPrincipal IUserPrincipal principal) {
        UUID owner = owners.of(principal);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", owner == null ? 0L : inbox.countUnread(owner))));
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllMineRead(
            @AuthenticationPrincipal IUserPrincipal principal) {
        UUID owner = owners.of(principal);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("updated", owner == null ? 0 : inbox.markAllRead(owner))));
    }
}
