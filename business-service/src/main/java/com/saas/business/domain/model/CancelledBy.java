package com.saas.business.domain.model;

/** Quien cerro la cita. Determina en que estado de cancelacion acaba. */
public enum CancelledBy {
    CLIENT,
    BUSINESS,
    /** Un proceso automatico: la ventana de confirmacion que se agoto. */
    SYSTEM
}
