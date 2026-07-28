package com.saas.search.application.dto.search;

/**
 * Cuerpo (POST) de una consulta de saldos en el read model. Sustituye los tres
 * GET que habia (por negocio, por usuario, por empleado): los criterios son
 * campos opcionales que se combinan, asi el front tiene UNA ruta.
 *
 * <pre>
 *   { "businessId": "..." }                  -> saldos del negocio
 *   { "businessId": "...", "branchId": "..." } -> saldos de una sede
 *   { "userId": "..." }                      -> el saldo de una cuenta (lista de 1)
 * </pre>
 *
 * Siempre responde una LISTA: quien espera uno solo toma el primero. Un unico
 * contrato evita duplicar el endpoint por cardinalidad.
 */
public record BalanceSearchRequest(
        String businessId,
        String branchId,
        String userId,
        String employeeId,
        Integer size
) {
    /** Techo de la lista: ningun negocio liquida mas empleados de una vez. */
    public static final int MAX_RESULTS = 500;

    public int sizeOrDefault() {
        return size == null || size <= 0 || size > MAX_RESULTS ? MAX_RESULTS : size;
    }
}
