package com.saas.common.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.security.IUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Emite el evento dedicado de auditoria ({@code audit.recorded}) al outbox.
 *
 * - Se sella el actor automaticamente desde el JWT (SecurityContext).
 * - Calcula los campos cambiados (diff de primer nivel) en UPDATE/TOGGLE.
 * - El payload es auto-contenido (before/after/actor): el audit-service no
 *   necesita llamar a nadie mas.
 *
 * Solo se activa donde existe un {@link OutboxPublisher} (servicios con outbox).
 */
@Slf4j
@Component
@ConditionalOnBean(OutboxPublisher.class)
@RequiredArgsConstructor
public class AuditEmitter {

    private final OutboxPublisher outbox;
    private final ObjectMapper mapper;

    /** Campos nunca auditados (defensa contra fuga de secretos). Comparacion case-insensitive. */
    private static final Set<String> REDACTED = Set.of(
            "password", "passwordhash", "password_hash", "pwd", "hash",
            "secret", "salt", "token", "accesstoken", "refreshtoken",
            "access_token", "refresh_token", "apikey", "api_key"
    );
    private static final String MASK = "***";

    /**
     * Captura un snapshot JSON inmutable de un objeto de dominio. Necesario en
     * UPDATE porque el servicio base muta la entidad existente en sitio antes
     * de persistir (el "before" se perderia si no se congela aqui).
     */
    public JsonNode snapshot(Object o) {
        return o == null ? null : mapper.valueToTree(o);
    }

    public void emit(AuditAction action,
                     String aggregateType,
                     UUID aggregateId,
                     UUID businessId,
                     Object before,
                     Object after) {
        try {
            JsonNode beforeNode = redact(before == null ? null : mapper.valueToTree(before));
            JsonNode afterNode  = redact(after  == null ? null : mapper.valueToTree(after));

            List<String> changed = (action == AuditAction.UPDATE || action == AuditAction.TOGGLE)
                    ? changedFields(beforeNode, afterNode)
                    : null;

            IUserPrincipal actor = currentActor();

            AuditPayload payload = AuditPayload.builder()
                    .action(action)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .businessId(businessId)
                    .actorId(actor != null ? actor.getUserId() : null)
                    .actorName(actor != null ? actor.getUsername() : null)
                    .occurredAt(Instant.now())
                    .before(beforeNode)
                    .after(afterNode)
                    .changedFields(changed)
                    .build();

            outbox.publish(EventTypes.AUDIT_RECORDED, businessId, aggregateType, aggregateId, payload);

        } catch (Exception ex) {
            // La auditoria nunca debe tumbar la operacion de negocio.
            log.error("No se pudo emitir evento de auditoria type={} aggregateId={}: {}",
                    aggregateType, aggregateId, ex.getMessage(), ex);
        }
    }

    /**
     * Regla anti-saturacion: si el usuario que modifica es el MISMO que creo el
     * registro, no se audita (evita auditorias "falsas" del propio autor).
     */
    public boolean isSelfModification(UUID createdBy) {
        if (createdBy == null) return false;
        IUserPrincipal actor = currentActor();
        return actor != null && createdBy.equals(actor.getUserId());
    }

    private IUserPrincipal currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        return (p instanceof IUserPrincipal up) ? up : null;
    }

    /** Enmascara recursivamente campos sensibles para que nunca lleguen al log de auditoria. */
    private JsonNode redact(JsonNode node) {
        if (node == null || !node.isContainerNode()) return node;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> it = obj.fieldNames();
            List<String> toMask = new ArrayList<>();
            while (it.hasNext()) {
                String f = it.next();
                if (REDACTED.contains(f.toLowerCase())) toMask.add(f);
                else redact(obj.get(f));
            }
            for (String f : toMask) obj.put(f, MASK);
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
        return node;
    }

    /** Diff de claves de primer nivel entre dos objetos JSON. */
    private List<String> changedFields(JsonNode before, JsonNode after) {
        List<String> changed = new ArrayList<>();
        if (after == null || !after.isObject()) return changed;
        ObjectNode a = (ObjectNode) after;
        Iterator<String> it = a.fieldNames();
        while (it.hasNext()) {
            String field = it.next();
            JsonNode bv = before != null ? before.get(field) : null;
            JsonNode av = a.get(field);
            if (bv == null ? av != null : !bv.equals(av)) changed.add(field);
        }
        // Campos que estaban antes y desaparecieron.
        if (before != null && before.isObject()) {
            Iterator<String> bit = before.fieldNames();
            while (bit.hasNext()) {
                String field = bit.next();
                if (!a.has(field)) changed.add(field);
            }
        }
        return changed;
    }
}
