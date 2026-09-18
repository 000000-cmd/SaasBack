package com.saas.events.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.saas.events.domain.model.Attachment;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API de Resend.
 *
 * Resend NO expone endpoint para consultar consumo: la cuota viaja en
 * cabeceras de cada respuesta (x-resend-monthly-quota, x-resend-daily-quota) y
 * el limite de tasa en ratelimit-*. Por eso cada llamada devuelve, ademas del
 * resultado, el retrato de cuota que venia en las cabeceras.
 */
@Slf4j
@Component
public class ResendClient {

    private static final String BASE = "https://api.resend.com";

    private final RestClient http = RestClient.builder().baseUrl(BASE).build();

    @Builder
    public record SendCommand(String apiKey, String from, List<String> to,
                              String replyTo, String subject, String html,
                              Attachment attachment) {}

    public record QuotaSnapshot(Integer monthlyUsed, Integer dailyUsed,
                                Integer rateLimit, Integer rateRemaining) {}

    public record SendResult(boolean ok, String messageId, String error, QuotaSnapshot quota) {}

    public SendResult send(SendCommand cmd) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", cmd.from());
        body.put("to", cmd.to());
        body.put("subject", cmd.subject() == null ? "" : cmd.subject());
        body.put("html", cmd.html());
        if (cmd.replyTo() != null && !cmd.replyTo().isBlank()) body.put("reply_to", cmd.replyTo());
        // Resend espera el binario ya en base64 bajo "content". El PDF llega asi
        // desde quien pidio el envio; este cliente no lo interpreta.
        if (cmd.attachment() != null && cmd.attachment().isUsable()) {
            body.put("attachments", List.of(Map.of(
                    "filename", cmd.attachment().filename(),
                    "content", cmd.attachment().contentBase64())));
        }

        try {
            var response = http.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cmd.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(JsonNode.class);

            QuotaSnapshot quota = readQuota(response.getHeaders());
            JsonNode json = response.getBody();
            String id = json != null && json.hasNonNull("id") ? json.get("id").asText() : null;
            return new SendResult(true, id, null, quota);

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // El cuerpo de error de Resend trae {"name":"...","message":"..."}.
            String detail = ex.getResponseBodyAsString();
            log.warn("Resend rechazo el envio: {} {}", ex.getStatusCode(), detail);
            return new SendResult(false, null,
                    truncate(ex.getStatusCode() + " " + detail), readQuota(ex.getResponseHeaders()));
        } catch (Exception ex) {
            log.error("Fallo llamando a Resend", ex);
            return new SendResult(false, null, truncate(ex.getMessage()), null);
        }
    }

    /**
     * Lectura inofensiva para refrescar las cabeceras sin enviar nada. Sirve
     * ademas para validar que la clave funciona antes del primer envio real.
     */
    public SendResult probe(String apiKey) {
        try {
            var response = http.get()
                    .uri("/domains")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .toEntity(JsonNode.class);
            return new SendResult(true, null, null, readQuota(response.getHeaders()));
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            return new SendResult(false, null,
                    truncate(ex.getStatusCode() + " " + ex.getResponseBodyAsString()),
                    readQuota(ex.getResponseHeaders()));
        } catch (Exception ex) {
            return new SendResult(false, null, truncate(ex.getMessage()), null);
        }
    }

    private static QuotaSnapshot readQuota(HttpHeaders h) {
        if (h == null) return null;
        return new QuotaSnapshot(
                intHeader(h, "x-resend-monthly-quota"),
                intHeader(h, "x-resend-daily-quota"),
                intHeader(h, "ratelimit-limit"),
                intHeader(h, "ratelimit-remaining"));
    }

    private static Integer intHeader(HttpHeaders h, String name) {
        String v = h.getFirst(name);
        if (v == null || v.isBlank()) return null;
        try { return Integer.valueOf(v.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** La columna Error es VARCHAR(500): recortar aqui evita un fallo al guardar la bitacora. */
    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 497) + "...";
    }

    /** Utilidad para construir la lista de destinatarios sin nulos ni vacios. */
    public static List<String> cleanRecipients(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String r : raw) if (r != null && !r.isBlank()) out.add(r.trim());
        return out;
    }
}
