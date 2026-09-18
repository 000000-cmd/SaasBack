package com.saas.finance.application.service;

import com.saas.finance.domain.model.PaymentMethod;
import com.saas.finance.domain.model.ServiceCharge;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static com.saas.finance.application.service.PdfCanvas.*;

/**
 * La FACTURA DEL CLIENTE: el papel que se lleva quien pago el servicio.
 *
 * <p>Sigue la pista "Boutique Elegant" del sistema de diseno — mucho blanco, una
 * sola tinta, jerarquia tipografica marcada — frente a la pista corporativa del
 * comprobante de nomina. Son dos publicos distintos: aqui se factura, alli se
 * rinde cuentas.</p>
 *
 * <p>NO va cifrada, al contrario que el extracto de nomina: se entrega al
 * cliente por correo o por un enlace, y no tenemos de el ningun dato que sirva
 * de contrasena y que ademas se sepa de memoria. El secreto es el propio enlace.</p>
 */
@Slf4j
@Component
public class ClientInvoicePdf {

    /**
     * Con LOCALE EXPLICITO. Sin el, el mes sale en el idioma de la maquina y en
     * el contenedor eso es ingles: la factura de un cliente colombiano no puede
     * decir "July" porque el servidor arranco con otra configuracion regional.
     */
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private static final float COL_WHO = RIGHT - 120f;
    private static final float COL_AMOUNT = RIGHT - 12f;

    /**
     * El numero de factura, derivado del id del cargo.
     *
     * <p>Se deriva en vez de guardarse en una columna con secuencia: el id ya es
     * unico e inmutable, y una secuencia propia obligaria a un contador por
     * negocio que hay que bloquear en cada venta. Si algun dia hace falta
     * numeracion fiscal consecutiva, esa columna existira por obligacion legal,
     * no por estetica.</p>
     */
    public static String numberOf(ServiceCharge charge) {
        String hex = charge.getId().toString().replace("-", "");
        return "FAC-" + hex.substring(0, 8).toUpperCase();
    }

    public byte[] build(ServiceCharge s, String employeeName, String businessName) {
        try (PDDocument pdf = new PDDocument()) {
            PdfCanvas c = new PdfCanvas(pdf);

            header(c, s, businessName);
            parties(c, s, businessName);
            items(c, s, employeeName);
            totals(c, s);
            closing(c, s);
            c.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();

        } catch (Exception ex) {
            log.error("No se pudo generar la factura del servicio {}: {}", s.getId(), ex.getMessage());
            return null;
        }
    }

    // =================================================================
    // Bloques
    // =================================================================

    private void header(PdfCanvas c, ServiceCharge s, String businessName) throws IOException {
        c.y = TOP;
        c.tracked(BOLD, 38f, LEFT, c.y, "FACTURA", PRIMARY, 2.5f);
        c.text(REGULAR, 9f, LEFT + 2, c.y - 16, "#" + numberOf(s), MUTED);

        // El estado, arriba a la derecha: es lo primero que se busca.
        c.badge(RIGHT, c.y + 12, "PAGADA", PRIMARY, WHITE);
        c.textRight(REGULAR, 8.5f, RIGHT, c.y - 6,
                "Emitida: " + s.getServiceDate().format(SHORT), MUTED);
        c.textRight(REGULAR, 8.5f, RIGHT, c.y - 18,
                "Medio de pago: " + medio(s.getPaymentMethod()), MUTED);

        c.y -= 44;
        c.line(LEFT, c.y, RIGHT, LINE);
        c.y -= 30;
    }

    private void parties(PdfCanvas c, ServiceCharge s, String businessName) throws IOException {
        float top = c.y;

        c.tracked(BOLD, 7.5f, LEFT, top, "FACTURADO A", PRIMARY, 0.9f);
        c.text(BOLD, 11f, LEFT, top - 16, nombreCliente(s), INK);
        float y = top - 30;
        if (notBlank(s.getClientEmail())) { c.text(REGULAR, 8.5f, LEFT, y, s.getClientEmail(), MUTED); y -= 12; }
        if (notBlank(s.getClientPhone())) { c.text(REGULAR, 8.5f, LEFT, y, s.getClientPhone(), MUTED); y -= 12; }

        c.tracked(BOLD, 7.5f, RIGHT - 180, top, "PRESTADOR DEL SERVICIO", PRIMARY, 0.9f);
        c.text(BOLD, 11f, RIGHT - 180, top - 16, businessName, INK);
        c.text(REGULAR, 8.5f, RIGHT - 180, top - 30,
                "Servicio del " + s.getServiceDate().format(DAY), MUTED);

        c.y = Math.min(y, top - 44) - 24;
    }

    private void items(PdfCanvas c, ServiceCharge s, String employeeName) throws IOException {
        c.tableHead("DESCRIPCIÓN DEL SERVICIO", "PROFESIONAL", "VALOR", COL_WHO, COL_AMOUNT);

        c.text(BOLD, 11f, LEFT + 12, c.y, safe(s.getServiceName()), INK);
        c.textRight(REGULAR, 8.5f, COL_WHO, c.y, safe(employeeName), MUTED);
        c.textRight(BOLD, 11f, COL_AMOUNT, c.y, cop(s.getGrossAmount()), INK);

        c.y -= 14;
        c.text(REGULAR, 8.5f, LEFT + 12, c.y, horario(s), MUTED);
        c.y -= 14;
        c.line(LEFT, c.y, RIGHT, LINE);
        c.y -= 28;
    }

    private void totals(PdfCanvas c, ServiceCharge s) throws IOException {
        float boxW = 240f, boxH = 86f;
        float boxX = RIGHT - boxW;
        float top = c.y;

        c.rect(boxX, top - boxH + 14, boxW, boxH, TINT);

        float y = top - 6;
        c.text(REGULAR, 9f, boxX + 16, y, "Subtotal", MUTED);
        c.textRight(REGULAR, 9f, RIGHT - 16, y, cop(s.getGrossAmount()), INK);
        y -= 17;
        c.text(REGULAR, 9f, boxX + 16, y, "Medio de pago", MUTED);
        c.textRight(REGULAR, 9f, RIGHT - 16, y, medio(s.getPaymentMethod()), INK);

        y -= 14;
        c.rect(boxX + 16, y, boxW - 32, 1.6f, PRIMARY);
        y -= 22;
        c.text(BOLD, 12f, boxX + 16, y, "TOTAL", PRIMARY);
        c.textRight(BOLD, 16f, RIGHT - 16, y - 2, cop(s.getGrossAmount()), PRIMARY);

        c.y = top - boxH - 26;
    }

    private void closing(PdfCanvas c, ServiceCharge s) throws IOException {
        c.termsBlock("GRACIAS POR TU VISITA", new String[] {
            "Este documento es el soporte del servicio prestado y del pago ya recibido.",
            "El valor incluye el servicio completo descrito arriba; no hay cargos adicionales.",
            "Conserva esta factura: es lo que respalda cualquier reclamo sobre el servicio.",
            "Factura " + numberOf(s) + " generada automáticamente. No requiere firma.",
        });
        c.footer("Documento electrónico · " + numberOf(s));
    }

    // =================================================================
    // Utilidades
    // =================================================================

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String nombreCliente(ServiceCharge s) {
        return notBlank(s.getClientName()) ? s.getClientName() : "Cliente";
    }

    private static String horario(ServiceCharge s) {
        String fecha = s.getServiceDate().format(DAY);
        if (s.getStartTime() == null) return fecha;
        String fin = s.getEndTime() == null ? "" : " a " + s.getEndTime().format(HOUR);
        return fecha + ", " + s.getStartTime().format(HOUR) + fin;
    }

    private static String medio(PaymentMethod m) {
        if (m == null) return "-";
        return switch (m) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case CARD -> "Tarjeta";
        };
    }
}
