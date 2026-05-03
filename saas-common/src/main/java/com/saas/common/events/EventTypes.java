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

    private EventTypes(){}
}
