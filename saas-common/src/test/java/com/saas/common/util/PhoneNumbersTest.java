package com.saas.common.util;

import org.junit.jupiter.api.Test;

/**
 * La comprobacion vive en {@link PhoneNumbers#check()}, junto al codigo que
 * comprueba: asi se lee entera de un vistazo y nadie tiene que abrir dos
 * archivos para saber que garantiza. Esto solo la ejecuta con el resto de la
 * bateria.
 */
class PhoneNumbersTest {

    @Test
    void elMismoTelefonoLlegueComoLlegueAcabaIgual() {
        PhoneNumbers.check();
    }
}
