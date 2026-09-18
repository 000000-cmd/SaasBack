package com.saas.finance.application.service;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.PayrollRun;
import com.saas.finance.domain.model.ServiceCharge;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.saas.finance.application.service.PdfCanvas.*;

/**
 * Comprobante de NOMINA en PDF, protegido con contrasena.
 *
 * <p>Sigue la pista "Elite Corporate" del sistema de diseno: titulo tipografico,
 * etiquetas en versalitas moradas, tabla con cabecera de color y el detalle
 * anidado bajo su concepto. Dibuja con {@link PdfCanvas}, el mismo lienzo que
 * usa la factura del cliente.</p>
 *
 * <p>La contrasena es el NUMERO DE DOCUMENTO del empleado: es lo unico que sabe
 * con certeza, no hay que comunicarselo por otro canal, y basta para que el
 * fichero no sea legible si el correo termina donde no debe.</p>
 *
 * <p>La comision NO se muestra como una cifra suelta: debajo van, uno a uno y
 * sin agrupar, los servicios que la componen con su fecha. "De donde sale este
 * numero" es la primera pregunta de quien recibe el papel, y responderla aqui
 * evita la conversacion entera.</p>
 */
@Slf4j
@Component
public class PayrollStatementPdf {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Columna de CANTIDAD y de MONTO, medidas por su borde derecho. */
    private static final float COL_QTY = RIGHT - 110f;
    private static final float COL_AMOUNT = RIGHT - 12f;

    /**
     * @param password numero de documento del empleado. Si viene vacio el PDF se
     *        genera SIN cifrar: mejor un extracto abierto que ningun extracto, y
     *        quien no tiene documento registrado es un caso a corregir en la
     *        ficha, no un motivo para dejarlo sin comprobante.
     */
    public byte[] build(PayrollRun run, EmployeeSettlement movement, List<ServiceCharge> services,
                        String employeeName, String employeeDocument, String businessName,
                        BigDecimal remainingBalance, String password) {
        try (PDDocument pdf = new PDDocument()) {
            PdfCanvas c = new PdfCanvas(pdf);

            header(c, run, movement, businessName);
            employee(c, movement, employeeName, employeeDocument, businessName);
            table(c, movement, services);
            totals(c, movement, remainingBalance);
            terms(c, run, movement);
            c.close();

            if (password != null && !password.isBlank()) {
                AccessPermission permissions = new AccessPermission();
                permissions.setCanModify(false);
                permissions.setCanModifyAnnotations(false);
                // Contrasena de propietario distinta de la del empleado: con la
                // suya puede leer e imprimir, no reescribir el documento.
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        java.util.UUID.randomUUID().toString(), password.trim(), permissions);
                policy.setEncryptionKeyLength(128);
                pdf.protect(policy);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();

        } catch (Exception ex) {
            // Que falle el papel no puede tumbar la nomina: el dinero ya se movio.
            log.error("No se pudo generar el extracto de {}: {}", run.getCode(), ex.getMessage());
            return null;
        }
    }

    // =================================================================
    // Bloques
    // =================================================================

    private void header(PdfCanvas c, PayrollRun run, EmployeeSettlement m, String businessName)
            throws IOException {
        c.y = TOP;
        c.tracked(BOLD, 30f, LEFT, c.y, "NÓMINA", INK, 5.5f);

        // Emisor, a la derecha y a la altura del titulo.
        c.textRight(BOLD, 9f, RIGHT, c.y + 14, up(businessName), INK);
        c.textRight(REGULAR, 8.5f, RIGHT, c.y + 1, "Comprobante de pago de nómina", MUTED);
        c.textRight(REGULAR, 8.5f, RIGHT, c.y - 11,
                m.getSettledAt() == null ? "-" : "Emitido " + m.getSettledAt().format(STAMP), MUTED);

        // Las dos pastillas del diseno: el periodo en gris, el folio en morado.
        c.y -= 26;
        float w1 = c.chip(LEFT, c.y, "PERIODO: " + up(run.getPeriodLabel()), CHIP, LINE, MUTED);
        c.chip(LEFT + w1 + 10, c.y, "FOLIO: " + up(run.getCode()), PRIMARY_SOFT, PRIMARY_SOFT, PRIMARY);

        c.y -= 22;
        c.line(LEFT, c.y, RIGHT, LINE);
        c.y -= 30;
    }

    private void employee(PdfCanvas c, EmployeeSettlement m, String name, String document,
                          String businessName) throws IOException {
        float top = c.y;

        c.text(BOLD, 12f, LEFT, c.y, "DETALLES DEL EMPLEADO", INK);
        c.y -= 8;
        c.line(LEFT, c.y, RIGHT - 200, LINE);
        c.y -= 20;

        float col2 = LEFT + 165;
        c.field(LEFT, c.y, "NOMBRE", name);
        c.field(col2, c.y, "DOCUMENTO", document);
        c.y -= 34;
        c.field(LEFT, c.y, "NEGOCIO", businessName);
        c.field(col2, c.y, "PAGO A",
                m.getPayoutAccount() == null || m.getPayoutAccount().isBlank()
                        ? "Consignación directa" : m.getPayoutAccount());

        // La unica cifra que se busca de un vistazo, en su caja morada.
        float boxW = 180f, boxH = 52f;
        float boxX = RIGHT - boxW;
        c.rect(boxX, top - boxH + 12, boxW, boxH, PRIMARY);
        c.textCenter(BOLD, 8f, boxX + boxW / 2, top - 4, "NETO A PAGAR", PRIMARY_SOFT);
        c.textCenter(BOLD, 19f, boxX + boxW / 2, top - 28, cop(m.getAmount()), WHITE);

        c.y -= 40;
    }

    private void table(PdfCanvas c, EmployeeSettlement m, List<ServiceCharge> services) throws IOException {
        c.tableHead("CONCEPTO / DESCRIPCIÓN", "CANTIDAD", "MONTO", COL_QTY, COL_AMOUNT);

        BigDecimal base = nz(m.getBaseSalaryAmount());
        if (base.compareTo(BigDecimal.ZERO) > 0) {
            c.row("Sueldo base del periodo", "1 periodo", cop(base), COL_QTY, COL_AMOUNT, false);
        }

        // La comision, con SUS servicios anidados debajo.
        BigDecimal comision = nz(m.getCommissionAmount());
        if (comision.compareTo(BigDecimal.ZERO) > 0 || !services.isEmpty()) {
            c.row("Comisión por servicios liquidados", String.valueOf(services.size()),
                    cop(comision), COL_QTY, COL_AMOUNT, true);

            boolean zebra = false;
            for (ServiceCharge s : services) {
                c.pageBreakIfNeeded(30);
                String fecha = s.getServiceDate() == null ? "" : s.getServiceDate().format(DAY);
                String detalle = safe(s.getServiceName())
                        + (s.getClientName() == null || s.getClientName().isBlank()
                            ? "" : "  -  " + safe(s.getClientName()));
                c.subRow(fecha, detalle, cop(s.getNetAmount()), COL_QTY, COL_AMOUNT, zebra);
                zebra = !zebra;
            }
        }

        c.y -= 14;
    }

    private void totals(PdfCanvas c, EmployeeSettlement m, BigDecimal remaining) throws IOException {
        c.pageBreakIfNeeded(120);
        float boxW = 250f, boxH = 96f;
        float boxX = RIGHT - boxW;
        float top = c.y;

        c.rect(boxX, top - boxH + 14, boxW, boxH, SOFT);
        c.stroke(boxX, top - boxH + 14, boxW, boxH, LINE);

        float y = top - 6;
        c.text(REGULAR, 9f, boxX + 16, y, "Comisión por servicios", MUTED);
        c.textRight(REGULAR, 9f, RIGHT - 16, y, cop(m.getCommissionAmount()), INK);
        y -= 16;
        c.text(REGULAR, 9f, boxX + 16, y, "Sueldo base", MUTED);
        c.textRight(REGULAR, 9f, RIGHT - 16, y, cop(m.getBaseSalaryAmount()), INK);
        y -= 16;
        c.text(REGULAR, 9f, boxX + 16, y, "Saldo a favor restante", MUTED);
        c.textRight(REGULAR, 9f, RIGHT - 16, y, cop(remaining), INK);

        // Regla morada de 2pt: el corte visual antes del total, tal cual el diseno.
        y -= 14;
        c.rect(boxX + 16, y, boxW - 32, 1.6f, PRIMARY);
        y -= 22;
        c.text(BOLD, 11f, boxX + 16, y, "TOTAL NETO", INK);
        c.textRight(BOLD, 15f, RIGHT - 16, y - 2, cop(m.getAmount()), PRIMARY);

        c.y = top - boxH - 18;
    }

    private void terms(PdfCanvas c, PayrollRun run, EmployeeSettlement m) throws IOException {
        String[] lineas = {
            "Recibí la cantidad neta indicada por concepto de sueldo y comisiones del periodo "
                + safe(run.getPeriodLabel()) + ".",
            "El detalle de arriba lista, uno a uno, cada servicio liquidado con su fecha y su valor neto.",
            Boolean.TRUE.equals(m.getPaidInCash())
                ? "Pago entregado EN EFECTIVO. Debe confirmarse desde la app del colaborador."
                : "Pago por transferencia. El comprobante bancario queda archivado en el sistema.",
            "El sistema no opera con el banco: la referencia de arriba es una constancia interna.",
            "Ante cualquier diferencia, contacta a la administración antes de 5 días hábiles.",
        };
        c.termsBlock("TÉRMINOS Y CONDICIONES", lineas);
        c.footer("Comprobante generado por el sistema · " + up(run.getCode()));
    }
}
