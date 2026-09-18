package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Aprobacion de un servicio. {@code receiptUrl} es opcional a proposito: el
 * dueno puede confirmar un pago electronico SIN comprobante (la pantalla se lo
 * advierte y el boton lo dice), porque bloquearlo dejaria servicios reales sin
 * pagar por un papel que a veces nunca llega.
 */
public record ConfirmChargeRequest(
        @Size(max = 500) String receiptUrl
) {}
