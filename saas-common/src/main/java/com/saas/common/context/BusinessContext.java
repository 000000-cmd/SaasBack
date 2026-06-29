package com.saas.common.context;

import java.util.UUID;

/**
 * Contexto de tenant por request (ThreadLocal). Lo llena {@link BusinessContextFilter}
 * a partir del header opcional {@code X-Business-Id} y lo consumen los servicios base
 * para sellar el {@code businessId} en eventos/auditoria, sin columnas extra ni lookups.
 */
public final class BusinessContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private BusinessContext() {}

    public static void set(UUID businessId) { CURRENT.set(businessId); }

    public static UUID get() { return CURRENT.get(); }

    public static void clear() { CURRENT.remove(); }
}
