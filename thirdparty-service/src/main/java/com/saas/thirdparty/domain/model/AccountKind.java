package com.saas.thirdparty.domain.model;

/**
 * Las dos formas de recibir plata.
 *
 * <p>{@link #BREV} guarda la llave TAL CUAL la escribio la persona. No se
 * normaliza, no se recorta y no se cambia de mayusculas: la llave es lo que el
 * banco resuelve, y tocarle un caracter manda la transferencia a otra parte o
 * la rebota.</p>
 */
public enum AccountKind {
    BANK,
    BREV
}
