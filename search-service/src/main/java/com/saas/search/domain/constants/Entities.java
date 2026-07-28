package com.saas.search.domain.constants;

public final class Entities {
    /** Convencion: valores siempre lowercase (matchea con parseEntities del ReindexService). */
    public static final String USER_ENTITY = "users";
    // Los roles NO viven en Elastic: media docena de filas fijas, sin busqueda
    // de texto ni join que evitar. El indice se retiro.
    public static final String LOCATION_ENTITY = "locations";
    public static final String THIRDPARTY_ENTITY = "third_parties";
    public static final String EMPLOYEE_BALANCE_ENTITY = "employee_balances";

    private Entities() {}
}
