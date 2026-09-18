package com.saas.finance.domain.model;

import java.time.LocalDate;

/**
 * Comprobacion de {@link PayrollFrequency}: la clave de periodo y sus bordes.
 *
 * <p>Merece prueba porque de aqui depende que el sueldo base NO se abone dos
 * veces. Si {@code periodKey} devolviera la misma cadena para dos quincenas
 * distintas, la clave unica de BD bloquearia el segundo abono y el empleado
 * cobraria de menos sin que nadie viera un error.</p>
 *
 * <p>Sin framework a proposito: se ejecuta con javac + java, sin levantar
 * Spring ni bajar dependencias.</p>
 *
 * <pre>
 * javac -d /tmp/out finance-service/src/main/java/com/saas/finance/domain/model/PayrollFrequency.java \
 *                   finance-service/src/test/java/com/saas/finance/domain/model/PayrollFrequencyCheck.java
 * java -cp /tmp/out com.saas.finance.domain.model.PayrollFrequencyCheck
 * </pre>
 */
public final class PayrollFrequencyCheck {

    private static int checks = 0;

    public static void main(String[] args) {
        LocalDate d05 = LocalDate.of(2026, 8, 5);    // 1ra quincena, semana 1
        LocalDate d15 = LocalDate.of(2026, 8, 15);   // ultimo dia de la 1ra quincena
        LocalDate d16 = LocalDate.of(2026, 8, 16);   // primer dia de la 2da
        LocalDate d31 = LocalDate.of(2026, 8, 31);   // ultimo del mes

        // --- Claves de periodo: dos fechas del mismo periodo comparten clave,
        //     dos de periodos distintos NO. Es toda la garantia anti-duplicado.
        eq("2026-08", PayrollFrequency.MONTHLY.periodKey(d05), "mensual: mismo mes, misma clave");
        eq("2026-08", PayrollFrequency.MONTHLY.periodKey(d31), "mensual: fin de mes sigue siendo el mismo mes");

        eq("2026-08-Q1", PayrollFrequency.BIWEEKLY.periodKey(d05), "quincenal: dia 5 es Q1");
        eq("2026-08-Q1", PayrollFrequency.BIWEEKLY.periodKey(d15), "quincenal: el 15 CIERRA Q1");
        eq("2026-08-Q2", PayrollFrequency.BIWEEKLY.periodKey(d16), "quincenal: el 16 ABRE Q2");
        ne(PayrollFrequency.BIWEEKLY.periodKey(d15), PayrollFrequency.BIWEEKLY.periodKey(d16),
                "quincenal: el borde 15/16 separa periodos");

        eq("2026-08-W1", PayrollFrequency.WEEKLY.periodKey(LocalDate.of(2026, 8, 7)), "semanal: dia 7 es W1");
        eq("2026-08-W2", PayrollFrequency.WEEKLY.periodKey(LocalDate.of(2026, 8, 8)), "semanal: dia 8 abre W2");

        // --- Bordes del periodo (los usa la corrida de nomina para etiquetarse)
        eq("2026-08-01", PayrollFrequency.MONTHLY.periodStart(d15).toString(), "mensual empieza el 1");
        eq("2026-08-31", PayrollFrequency.MONTHLY.periodEnd(d15).toString(), "mensual acaba el ultimo dia");
        eq("2026-08-01", PayrollFrequency.BIWEEKLY.periodStart(d15).toString(), "Q1 empieza el 1");
        eq("2026-08-15", PayrollFrequency.BIWEEKLY.periodEnd(d15).toString(), "Q1 acaba el 15");
        eq("2026-08-16", PayrollFrequency.BIWEEKLY.periodStart(d16).toString(), "Q2 empieza el 16");
        eq("2026-08-31", PayrollFrequency.BIWEEKLY.periodEnd(d16).toString(), "Q2 acaba el ultimo dia");
        // La ultima semana se recorta al mes: no puede desbordar a septiembre.
        eq("2026-08-31", PayrollFrequency.WEEKLY.periodEnd(LocalDate.of(2026, 8, 30)).toString(),
                "semanal: la ultima semana se recorta al mes");

        // --- Febrero, que es donde revientan los calendarios ingenuos
        eq("2026-02-28", PayrollFrequency.MONTHLY.periodEnd(LocalDate.of(2026, 2, 10)).toString(),
                "febrero no bisiesto acaba el 28");
        eq("2024-02-29", PayrollFrequency.MONTHLY.periodEnd(LocalDate.of(2024, 2, 10)).toString(),
                "febrero bisiesto acaba el 29");

        // --- Fraccion del sueldo: 1.000.000 mensual son 500.000 por quincena
        eqInt(1, PayrollFrequency.MONTHLY.getPeriodsPerMonth(), "mensual: 1 periodo");
        eqInt(2, PayrollFrequency.BIWEEKLY.getPeriodsPerMonth(), "quincenal: 2 periodos");
        eqInt(4, PayrollFrequency.WEEKLY.getPeriodsPerMonth(), "semanal: 4 periodos");

        // --- Tolerancia: configuracion vacia o basura no puede tumbar la tarea
        eq("MONTHLY", PayrollFrequency.from(null).name(), "sin configuracion: mensual");
        eq("MONTHLY", PayrollFrequency.from("  ").name(), "vacio: mensual");
        eq("MONTHLY", PayrollFrequency.from("cualquier-cosa").name(), "valor invalido: mensual");
        eq("BIWEEKLY", PayrollFrequency.from("biweekly").name(), "minusculas se aceptan");

        System.out.println("OK - " + checks + " comprobaciones de PayrollFrequency");
    }

    private static void eq(String expected, String actual, String what) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(what + " -> esperaba '" + expected + "' y llego '" + actual + "'");
        }
    }

    private static void eqInt(int expected, int actual, String what) {
        eq(String.valueOf(expected), String.valueOf(actual), what);
    }

    private static void ne(String a, String b, String what) {
        checks++;
        if (a.equals(b)) throw new AssertionError(what + " -> ambos dieron '" + a + "'");
    }

    private PayrollFrequencyCheck() {}
}
