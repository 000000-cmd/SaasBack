package com.saas.finance.application.service;

import com.saas.common.events.EventTypes;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.finance.application.dto.event.NotificationRequestPayload;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.out.IEmployeeBalanceRepositoryPort;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import com.saas.finance.infrastructure.client.BusinessInternalClient;
import com.saas.finance.infrastructure.client.ThirdPartyInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La factura del cliente: generarla y hacersela llegar.
 *
 * <p>Sale sola al APROBAR el servicio, no al crearlo: hasta que el dueno no lo
 * revisa, el servicio todavia puede descartarse, y una factura que hay que
 * anular despues es peor que una factura que llega un rato mas tarde.</p>
 *
 * <p>Por correo va el PDF ADJUNTO. Por SMS o WhatsApp va un ENLACE al back que
 * la genera en el momento: en esos canales no se pueden mandar ficheros, y
 * guardar el PDF en disco para poder enlazarlo seria un fichero mas que
 * respaldar cuando el documento es una vista de datos que ya estan congelados
 * en el cargo.</p>
 *
 * <p>NADA de lo que pasa aqui puede tumbar la aprobacion del servicio: si falla
 * el correo o el PDF, se registra y se sigue.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientInvoiceService {

    private static final String AGGREGATE_TYPE = "service_charge";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IServiceChargeRepositoryPort charges;
    private final IEmployeeBalanceRepositoryPort balances;
    private final ClientInvoicePdf invoices;
    private final ThirdPartyInternalClient people;
    private final BusinessInternalClient businesses;
    private final OutboxPublisher outbox;

    /**
     * Raiz publica desde la que el cliente descarga su factura. Es el gateway,
     * no el servicio: el enlace viaja en un SMS a un telefono que esta fuera de
     * la red interna.
     */
    @Value("${saas.public.base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /** El PDF de un cargo, listo para descargar. */
    @Transactional(readOnly = true)
    public byte[] pdf(UUID chargeId) {
        ServiceCharge c = charges.findById(chargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio prestado", "id", chargeId));
        return invoices.build(c, employeeName(c), businessName(c.getBusinessId()));
    }

    /** El nombre de fichero con el que se descarga o se adjunta. */
    public String filename(UUID chargeId) {
        return "factura-" + ClientInvoicePdf.numberOf(
                charges.findById(chargeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Servicio prestado", "id", chargeId)))
                .toLowerCase() + ".pdf";
    }

    /**
     * Se la manda al cliente por donde se pueda: correo con el PDF adjunto,
     * telefono con el enlace de descarga. Si no dejo ningun contacto no se
     * intenta nada — y eso no es un error, es un cliente de paso.
     */
    // NO va readOnly aunque solo lea el cargo: publicar al outbox es un INSERT, y
    // en una transaccion de solo lectura Hibernate no vacia la sesion — la fila se
    // pierde SIN ERROR y el aviso nunca sale. Es el fallo mas caro de diagnosticar
    // que hay aqui: todo responde 200 y no llega nada.
    @Transactional
    public void send(UUID chargeId) {
        try {
            ServiceCharge c = charges.findById(chargeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio prestado", "id", chargeId));
            send(c);
        } catch (Exception ex) {
            log.warn("No se pudo enviar la factura del servicio {}: {}", chargeId, ex.getMessage());
        }
    }

    /** Igual que el anterior, con el cargo ya cargado: lo usa la aprobacion. */
    public void send(ServiceCharge c) {
        try {
            List<String> destinos = new ArrayList<>();
            if (notBlank(c.getClientEmail())) destinos.add(c.getClientEmail().trim());
            if (notBlank(c.getClientPhone())) destinos.add(c.getClientPhone().trim());
            if (destinos.isEmpty()) {
                log.debug("Servicio {} sin contacto del cliente; no se manda factura", c.getId());
                return;
            }

            String negocio = businessName(c.getBusinessId());
            String numero = ClientInvoicePdf.numberOf(c);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("CLIENTE", c.getClientName() == null || c.getClientName().isBlank()
                    ? "Hola" : c.getClientName());
            data.put("NEGOCIO", negocio);
            data.put("SERVICIO", c.getServiceName());
            data.put("EMPLEADO", employeeName(c));
            data.put("FECHA", c.getServiceDate() == null ? "-" : c.getServiceDate().format(DAY));
            data.put("MONTO", cop(c.getGrossAmount()));
            data.put("FACTURA", numero);
            data.put("LINK", downloadLink(c.getId()));

            NotificationRequestPayload.Attachment adjunto = null;
            byte[] pdf = invoices.build(c, employeeName(c), negocio);
            if (pdf != null) {
                adjunto = new NotificationRequestPayload.Attachment(
                        "factura-" + numero.toLowerCase() + ".pdf",
                        Base64.getEncoder().encodeToString(pdf));
            }

            // El cliente no tiene bandeja en la app: thirdPartyId va nulo salvo
            // que sea alguien registrado. Escribir bandeja de un desconocido no
            // se la mostraria a nadie.
            outbox.publish(EventTypes.NOTIFICATION_REQUESTED, c.getBusinessId(),
                    AGGREGATE_TYPE, c.getId(),
                    new NotificationRequestPayload("CLIENT_INVOICE", destinos,
                            c.getClientThirdPartyId(), data, adjunto));

            log.info("Factura {} encolada para {} destinos", numero, destinos.size());

        } catch (Exception ex) {
            log.warn("No se pudo encolar la factura del servicio {}: {}", c.getId(), ex.getMessage());
        }
    }

    /**
     * El enlace de descarga.
     *
     * <p>ponytail: el secreto es el propio id del cargo, que es un UUID
     * aleatorio — 122 bits, no se adivina ni se enumera. Se evita asi una tabla
     * de tokens con su caducidad y su limpieza. Si algun dia la factura debe
     * caducar o revocarse, ahi hara falta el token de verdad.</p>
     */
    public String downloadLink(UUID chargeId) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/finance/public/invoices/" + chargeId;
    }

    // ---- utilidades ----

    /**
     * El nombre del profesional que atendio.
     *
     * <p>Se saca del SALDO porque es la unica fila de finance que enlaza empleado
     * con tercero; el nombre en si vive en thirdparty y se pide alli. Si no hay
     * saldo todavia, la factura sale sin nombre propio en vez de no salir.</p>
     */
    private String employeeName(ServiceCharge c) {
        try {
            UUID thirdPartyId = balances.findByEmployeeId(c.getEmployeeId())
                    .map(EmployeeBalance::getThirdPartyId).orElse(null);
            if (thirdPartyId == null) return "Nuestro equipo";
            ThirdPartyInternalClient.NotifyTarget t =
                    people.notifyTargets(Set.of(thirdPartyId)).get(thirdPartyId.toString());
            return t == null || t.fullName() == null ? "Nuestro equipo" : t.fullName();
        } catch (Exception ex) {
            log.debug("Sin nombre de profesional para el servicio {}: {}", c.getId(), ex.getMessage());
            return "Nuestro equipo";
        }
    }

    private String businessName(UUID businessId) {
        try {
            return businesses.name(businessId).name();
        } catch (Exception ex) {
            log.debug("Sin nombre de negocio para {}: {}", businessId, ex.getMessage());
            return "Tu negocio";
        }
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String cop(java.math.BigDecimal v) {
        java.math.BigDecimal n = v == null ? java.math.BigDecimal.ZERO : v;
        return "$ " + String.format("%,.0f", n).replace(',', '.');
    }
}
