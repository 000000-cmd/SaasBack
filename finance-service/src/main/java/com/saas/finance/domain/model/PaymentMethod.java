package com.saas.finance.domain.model;

/**
 * Como cobro el cliente ese servicio.
 *
 * <p>Importa por una sola razon: los dos medios ELECTRONICOS exigen comprobante.
 * Sin el, el dueno tendria que entrar a su banco a comprobar a mano que la plata
 * llego, y por eso la pantalla los marca aparte.</p>
 */
public enum PaymentMethod {
    CASH,
    TRANSFER,
    CARD;

    /** Si exige comprobante para poder confirmar sin dudas. */
    public boolean isElectronic() {
        return this != CASH;
    }
}
