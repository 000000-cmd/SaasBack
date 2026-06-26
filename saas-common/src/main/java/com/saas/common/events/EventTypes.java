package com.saas.common.events;

// Cuando salga a produccion ya no se va a poder cambiarle el nombre, toca crear otro y se deja el viejo deprecated porque los consumer buscan nombre exacto
public final class EventTypes {
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_ROLES_CHANGED = "user.roles.changed";

    public static final String ROLE_CREATED = "role.created";
    public static final String ROLE_UPDATED = "role.updated";
    public static final String ROLE_DELETED = "role.deleted";

    public static final String MENU_CREATED = "menu.created";
    public static final String MENU_UPDATED = "menu.updated";
    public static final String MENU_DELETED = "menu.deleted";

    public static final String PERSON_CREATED = "person.created";
    public static final String PERSON_UPDATED = "person.updated";
    public static final String PERSON_DELETED = "person.deleted";
    public static final String PERSON_BUSINESS_ASSOCIATED = "person.business.associated";

    public static final String LOCATION_COUNTRY_CREATED = "location.country.created";
    public static final String LOCATION_COUNTRY_UPDATED = "location.country.updated";
    public static final String LOCATION_COUNTRY_DELETED = "location.country.deleted";

    public static final String LOCATION_DEPARTMENT_CREATED = "location.department.created";
    public static final String LOCATION_DEPARTMENT_UPDATED = "location.department.updated";
    public static final String LOCATION_DEPARTMENT_DELETED = "location.department.deleted";

    public static final String LOCATION_MUNICIPALITY_CREATED = "location.municipality.created";
    public static final String LOCATION_MUNICIPALITY_UPDATED = "location.municipality.updated";
    public static final String LOCATION_MUNICIPALITY_DELETED = "location.municipality.deleted";

    public static final String LOCATION_NEIGHBORHOOD_CREATED = "location.neighborhood.created";
    public static final String LOCATION_NEIGHBORHOOD_UPDATED = "location.neighborhood.updated";
    public static final String LOCATION_NEIGHBORHOOD_DELETED = "location.neighborhood.deleted";

    public static final String TENANT_CREATED = "tenant.created";
    public static final String TENANT_UPDATED = "tenant.updated";
    public static final String TENANT_DELETED = "tenant.deleted";

    /**
     * Evento dedicado de auditoria. Lo emite {@link com.saas.common.audit.AuditEmitter}
     * desde los servicios base CRUD. El relay enruta todo lo que empiece por
     * "audit." al topic dedicado {@code audit.events} (ver OutboxRelay).
     */
    public static final String AUDIT_RECORDED = "audit.recorded";
    /** Prefijo usado por el relay para enrutar al topic de auditoria. */
    public static final String AUDIT_PREFIX = "audit.";

    private EventTypes(){}
}
