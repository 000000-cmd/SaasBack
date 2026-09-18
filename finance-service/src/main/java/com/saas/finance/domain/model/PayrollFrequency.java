package com.saas.finance.domain.model;

import java.time.LocalDate;

/**
 * Cada cuanto la empresa dispersa nomina. Se configura a nivel de NEGOCIO
 * (business_compensation): es una politica de la empresa, no de una persona.
 *
 * <p>De aqui sale en cuantas partes se fracciona el sueldo base al abonarse al
 * saldo. Si la empresa paga quincenal y el sueldo base es de 1.000.000, al
 * empleado le aparecen 500.000 al inicio de cada quincena, no el millon
 * completo: el saldo debe reflejar lo que se le va a consignar, no lo que
 * ganara a fin de mes.</p>
 */
public enum PayrollFrequency {

    MONTHLY(1),
    BIWEEKLY(2),
    WEEKLY(4);

    private final int periodsPerMonth;

    PayrollFrequency(int periodsPerMonth) {
        this.periodsPerMonth = periodsPerMonth;
    }

    public int getPeriodsPerMonth() {
        return periodsPerMonth;
    }

    /** Tolerante a datos viejos o vacios: sin configuracion se asume mensual. */
    public static PayrollFrequency from(String raw) {
        if (raw == null || raw.isBlank()) return MONTHLY;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MONTHLY;
        }
    }

    /**
     * Clave del periodo al que pertenece una fecha: {@code 2026-08},
     * {@code 2026-08-Q1}, {@code 2026-08-W3}.
     *
     * <p>Es la que hace idempotente el abono del sueldo base: la unica de
     * employee_settlement (EmployeeId, MovementType, PeriodKey) impide que la
     * tarea programada lo abone dos veces aunque corra mil veces.</p>
     */
    public String periodKey(LocalDate date) {
        String month = "%d-%02d".formatted(date.getYear(), date.getMonthValue());
        return switch (this) {
            case MONTHLY -> month;
            case BIWEEKLY -> month + (date.getDayOfMonth() <= 15 ? "-Q1" : "-Q2");
            // Semanas del MES (1..5), no del ano: la clave debe ser legible al
            // lado de las otras dos y unica dentro del mes ya es suficiente.
            case WEEKLY -> month + "-W" + (((date.getDayOfMonth() - 1) / 7) + 1);
        };
    }

    /** Primer dia del periodo que contiene a {@code date}. */
    public LocalDate periodStart(LocalDate date) {
        return switch (this) {
            case MONTHLY -> date.withDayOfMonth(1);
            case BIWEEKLY -> date.withDayOfMonth(date.getDayOfMonth() <= 15 ? 1 : 16);
            case WEEKLY -> date.withDayOfMonth((((date.getDayOfMonth() - 1) / 7) * 7) + 1);
        };
    }

    /** Ultimo dia del periodo que contiene a {@code date}. */
    public LocalDate periodEnd(LocalDate date) {
        LocalDate lastOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        return switch (this) {
            case MONTHLY -> lastOfMonth;
            case BIWEEKLY -> date.getDayOfMonth() <= 15 ? date.withDayOfMonth(15) : lastOfMonth;
            case WEEKLY -> {
                LocalDate end = periodStart(date).plusDays(6);
                yield end.isAfter(lastOfMonth) ? lastOfMonth : end;
            }
        };
    }

    /** Etiqueta para el dueno y para el correo: "1ra quincena de agosto 2026". */
    public String periodLabel(LocalDate date) {
        String month = MONTHS[date.getMonthValue() - 1] + " " + date.getYear();
        return switch (this) {
            case MONTHLY -> "Mes de " + month;
            case BIWEEKLY -> (date.getDayOfMonth() <= 15 ? "1ra" : "2da") + " quincena de " + month;
            case WEEKLY -> "Semana " + (((date.getDayOfMonth() - 1) / 7) + 1) + " de " + month;
        };
    }

    private static final String[] MONTHS = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };
}
