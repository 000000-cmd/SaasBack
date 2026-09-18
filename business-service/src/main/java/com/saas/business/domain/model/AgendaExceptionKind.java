package com.saas.business.domain.model;

/**
 * Por que se resta ese tiempo de la agenda.
 *
 * <p>No cambia el calculo —todas restan igual— pero si lo que se ve y lo que
 * se puede decidir despues: "cerrado por festivo" y "el barbero esta de
 * vacaciones" se pintan distinto en el calendario, y las vacaciones y las
 * incapacidades acaban interesandole a nomina.</p>
 */
public enum AgendaExceptionKind {
    HOLIDAY, VACATION, SICK, BLOCK, OTHER;

    public String legible() {
        return switch (this) {
            case HOLIDAY -> "Festivo";
            case VACATION -> "Vacaciones";
            case SICK -> "Incapacidad";
            case BLOCK -> "Bloqueo";
            case OTHER -> "Otro";
        };
    }
}
