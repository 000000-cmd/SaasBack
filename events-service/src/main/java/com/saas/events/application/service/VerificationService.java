package com.saas.events.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.VerificationCode;
import com.saas.events.domain.port.out.IVerificationCodeRepositoryPort;
import com.saas.events.infrastructure.client.ThirdPartyInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verificacion de un contacto por codigo de un solo uso.
 *
 * GENERICO POR CONSTRUCCION: el codigo sale por el MISMO despachador que
 * cualquier otra notificacion, resolviendo una plantilla con {@code {{CODIGO}}}.
 * Por eso SMS y WhatsApp no necesitaran desarrollo extra: el dia que exista su
 * proveedor, el codigo sale por ahi porque este servicio nunca supo por donde
 * salia. El canal se decide por el TIPO DE CONTACTO, con un mapa, no con ifs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    /** Notificacion del sistema que transporta el codigo. Debe existir con una plantilla por canal. */
    public static final String NOTIFICATION_CODE = "CONTACT_VERIFY";
    public static final String PURPOSE = "CONTACT_VERIFY";

    /** Tipo de contacto -> canal de envio. Un mapa, no una cadena de ifs. */
    private static final Map<String, ChannelType> CANAL_POR_CONTACTO = Map.of(
            "EMAIL", ChannelType.EMAIL,
            "MOBILE", ChannelType.SMS,
            "WHATSAPP", ChannelType.WHATSAPP);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final IVerificationCodeRepositoryPort codes;
    private final ThirdPartyInternalClient thirdParty;
    private final DispatchService dispatch;
    private final com.saas.events.domain.port.out.INotificationRepositoryPort notifications;
    private final com.saas.events.domain.port.out.INotificationTemplateRepositoryPort templates;

    @Value("${saas.verification.length:6}")           private int length;
    @Value("${saas.verification.ttl-minutes:10}")     private int ttlMinutes;
    @Value("${saas.verification.max-attempts:5}")     private int maxAttempts;
    @Value("${saas.verification.resend-seconds:60}")  private int resendSeconds;

    public record Requested(String target, String channelCode, LocalDateTime expiresAt, int resendInSeconds) {}

    /**
     * Emite un codigo y lo manda por el canal que corresponda al contacto.
     *
     * Un solo codigo activo por destino: pedir uno nuevo invalida los anteriores.
     * Si no, cinco correos abiertos darian cinco codigos validos a la vez.
     */
    @Transactional
    public Requested request(UUID contactId) {
        ThirdPartyInternalClient.ContactDetail contact = thirdParty.contact(contactId);
        if (contact == null || contact.value() == null || contact.value().isBlank()) {
            throw new BusinessException("El contacto no existe o no tiene valor");
        }
        if (Boolean.TRUE.equals(contact.isVerified())) {
            throw new BusinessException("Este contacto ya está verificado");
        }

        ChannelType channel = CANAL_POR_CONTACTO.get(contact.typeCode());
        if (channel == null) {
            throw new BusinessException("Este tipo de contacto no se puede verificar: " + contact.typeCode());
        }

        // Sin plantilla para ese canal no hay nada que enviar. Se comprueba ANTES
        // de crear el codigo: si no, se emitiria un codigo que nunca sale y la
        // pantalla diria "codigo enviado" mintiendo. Es exactamente lo que pasa
        // hoy con SMS y WhatsApp, que todavia no tienen plantilla propia.
        boolean hayPlantilla = notifications.findByCode(NOTIFICATION_CODE)
                .map(n -> templates.findByNotificationId(n.getId()).stream()
                        .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                        .anyMatch(t -> channel == ChannelType.from(t.getTypeCode())))
                .orElse(false);
        if (!hayPlantilla) {
            throw new BusinessException(
                    "Todavía no se puede verificar por " + canalLegible(channel)
                    + ": falta la plantilla de ese canal en la notificación " + NOTIFICATION_CODE + ".");
        }

        List<VerificationCode> vivos = codes.findAllActive(contact.value(), PURPOSE);

        // Espera entre reenvios: sin ella el boton "reenviar" es un amplificador
        // para inundar el buzon de alguien.
        LocalDateTime ahora = LocalDateTime.now();
        for (VerificationCode v : vivos) {
            if (v.getCreatedDate() != null
                    && Duration.between(v.getCreatedDate(), ahora).getSeconds() < resendSeconds) {
                long faltan = resendSeconds - Duration.between(v.getCreatedDate(), ahora).getSeconds();
                throw new BusinessException("Espera " + faltan + " segundos para pedir otro código");
            }
        }
        vivos.forEach(v -> { v.setConsumedAt(ahora); codes.save(v); });

        String plano = generar();
        VerificationCode nuevo = codes.save(VerificationCode.builder()
                .target(contact.value())
                .channelCode(channel.name())
                .purpose(PURPOSE)
                .codeHash(hash(plano))
                .expiresAt(ahora.plusMinutes(ttlMinutes))
                .attempts(0)
                .maxAttempts(maxAttempts)
                .contactId(contactId)
                .build());

        // Sale por el camino normal: misma plantilla, mismo renderizador, misma
        // bitacora. El codigo es un parametro mas.
        dispatch.dispatch(NOTIFICATION_CODE, List.of(contact.value()),
                Map.of("CODIGO", plano, "MINUTOS", String.valueOf(ttlMinutes)),
                null, contact.thirdPartyId());

        return new Requested(enmascarar(contact.value(), contact.typeCode()),
                channel.name(), nuevo.getExpiresAt(), resendSeconds);
    }

    /**
     * Comprueba el codigo. Al acertar, sella el contacto como verificado.
     *
     * Al agotar los intentos el codigo se CONSUME y hay que pedir otro: seis
     * digitos son un millon de combinaciones, pero sin limite se agotan en
     * minutos.
     */
    @Transactional
    public boolean verify(UUID contactId, String code) {
        ThirdPartyInternalClient.ContactDetail contact = thirdParty.contact(contactId);
        if (contact == null) throw new BusinessException("El contacto no existe");

        VerificationCode activo = codes.findActive(contact.value(), PURPOSE)
                .orElseThrow(() -> new BusinessException("No hay ningún código pendiente. Pide uno nuevo."));

        if (activo.isExpired()) {
            activo.setConsumedAt(LocalDateTime.now());
            codes.save(activo);
            throw new BusinessException("El código venció. Pide uno nuevo.");
        }

        activo.setAttempts(activo.getAttempts() + 1);

        if (!constantTimeEquals(hash(code == null ? "" : code.trim()), activo.getCodeHash())) {
            if (!activo.hasAttemptsLeft()) {
                activo.setConsumedAt(LocalDateTime.now());
                codes.save(activo);
                throw new BusinessException("Demasiados intentos. Pide un código nuevo.");
            }
            codes.save(activo);
            int restantes = activo.getMaxAttempts() - activo.getAttempts();
            throw new BusinessException("Código incorrecto. Te quedan " + restantes + " intentos.");
        }

        activo.setConsumedAt(LocalDateTime.now());
        codes.save(activo);
        thirdParty.verifyContact(contactId);
        log.info("Contacto {} verificado por codigo", contactId);
        return true;
    }

    /** Nombre del canal como lo diría alguien, para que el aviso se entienda. */
    private static String canalLegible(ChannelType c) {
        return switch (c) {
            case EMAIL -> "correo electrónico";
            case SMS -> "mensaje de texto";
            case WHATSAPP -> "WhatsApp";
            case PUSH -> "notificación al teléfono";
        };
    }

    private String generar() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * Comparacion en tiempo constante: comparar con equals filtra informacion
     * por el tiempo de respuesta, que es como se adivina un secreto sin verlo.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    /** Se devuelve enmascarado para confirmar a donde fue sin exponerlo entero. */
    private static String enmascarar(String value, String typeCode) {
        if (value == null || value.isBlank()) return "";
        if ("EMAIL".equals(typeCode)) {
            int at = value.indexOf('@');
            if (at <= 1) return "•••" + value.substring(Math.max(at, 0));
            return value.charAt(0) + "•••" + value.substring(at - 1);
        }
        return value.length() <= 4 ? "••••" : "••••" + value.substring(value.length() - 4);
    }
}
