package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.service.ClientInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La descarga de la factura del cliente. PUBLICA a proposito.
 *
 * <p>Es el destino del enlace que se manda por SMS o WhatsApp, canales donde no
 * se puede adjuntar un fichero. Quien lo abre es un cliente del salon: no tiene
 * cuenta, y pedirle que se registre para ver lo que ya pago no tiene sentido.</p>
 *
 * <p>El secreto es el propio identificador del cargo, que es un UUID aleatorio:
 * no se adivina ni se enumera. Devuelve el PDF y nada mas — ni JSON, ni datos
 * del negocio, ni lista de nada.</p>
 */
@RestController
@RequestMapping("/public/invoices")
@RequiredArgsConstructor
public class PublicInvoiceController {

    private final ClientInvoiceService invoices;

    @GetMapping("/{chargeId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID chargeId) {
        byte[] pdf = invoices.pdf(chargeId);
        if (pdf == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                // inline: en el movil se abre en el visor sin pasar por la
                // carpeta de descargas, que es donde una factura se pierde.
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + invoices.filename(chargeId) + "\"")
                .body(pdf);
    }
}
