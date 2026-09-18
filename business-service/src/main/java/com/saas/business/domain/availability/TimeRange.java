package com.saas.business.domain.availability;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Un intervalo de tiempo, con el final EXCLUIDO.
 *
 * <p>Que el final se excluya no es un detalle: una cita de 10:00 a 10:30 y otra
 * de 10:30 a 11:00 NO se solapan, y con el final incluido se solaparian en un
 * instante. Ese instante es la diferencia entre una agenda que encaja y una que
 * rechaza la mitad de las reservas seguidas.</p>
 *
 * <p>Todo en UTC. La conversion a la hora del negocio es cosa de la
 * presentacion; aqui dentro no hay husos ni horarios de verano que puedan
 * cambiar el resultado de una resta.</p>
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Un intervalo necesita principio y fin");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("El fin va despues del principio: " + start + " → " + end);
        }
    }

    public Duration length() {
        return Duration.between(start, end);
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** {@code true} si {@code other} cabe entero aqui dentro. */
    public boolean contains(TimeRange other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    public boolean containsInstant(Instant t) {
        return !t.isBefore(start) && t.isBefore(end);
    }

    /**
     * Une los intervalos que se tocan o se solapan, y los deja ordenados.
     *
     * <p>Hace falta antes de restar: dos turnos pegados (mañana 8-12, tarde
     * 12-18) son en la practica uno solo de 8 a 18, y tratarlos por separado
     * partiria en dos un servicio que empieza a las 11:45.</p>
     */
    public static List<TimeRange> merge(List<TimeRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return List.of();

        List<TimeRange> ordenados = new ArrayList<>(ranges);
        ordenados.sort(Comparator.comparing(TimeRange::start));

        List<TimeRange> out = new ArrayList<>();
        TimeRange actual = ordenados.get(0);
        for (int i = 1; i < ordenados.size(); i++) {
            TimeRange siguiente = ordenados.get(i);
            // "No empieza despues de que termine" y no "se solapa": dos tramos
            // que se tocan exactamente (12:00-12:00) tambien se unen.
            if (!siguiente.start().isAfter(actual.end())) {
                Instant fin = actual.end().isAfter(siguiente.end()) ? actual.end() : siguiente.end();
                actual = new TimeRange(actual.start(), fin);
            } else {
                out.add(actual);
                actual = siguiente;
            }
        }
        out.add(actual);
        return out;
    }

    /**
     * Quita de {@code libres} todo lo que ocupen {@code ocupados}.
     *
     * <p>Es la operacion central del motor: el horario abierto menos lo que ya
     * esta cogido. Devuelve los trozos que quedan, en orden, sin vacios de
     * longitud cero.</p>
     */
    public static List<TimeRange> subtract(List<TimeRange> libres, List<TimeRange> ocupados) {
        if (libres == null || libres.isEmpty()) return List.of();
        if (ocupados == null || ocupados.isEmpty()) return merge(libres);

        List<TimeRange> bloqueos = merge(ocupados);
        List<TimeRange> resultado = new ArrayList<>();

        for (TimeRange libre : merge(libres)) {
            Instant cursor = libre.start();
            for (TimeRange ocupado : bloqueos) {
                if (!ocupado.overlaps(new TimeRange(cursor, libre.end()))) continue;

                // El trozo que queda antes de este bloqueo, si queda algo.
                if (ocupado.start().isAfter(cursor)) {
                    resultado.add(new TimeRange(cursor, ocupado.start()));
                }
                // El cursor salta al final del bloqueo, nunca hacia atras.
                if (ocupado.end().isAfter(cursor)) {
                    cursor = ocupado.end();
                }
                if (!cursor.isBefore(libre.end())) break;
            }
            if (cursor.isBefore(libre.end())) {
                resultado.add(new TimeRange(cursor, libre.end()));
            }
        }
        return resultado;
    }

    /** Los trozos comunes a las dos listas. El horario del local ∩ el turno. */
    public static List<TimeRange> intersect(List<TimeRange> a, List<TimeRange> b) {
        List<TimeRange> out = new ArrayList<>();
        for (TimeRange x : merge(a)) {
            for (TimeRange y : merge(b)) {
                if (!x.overlaps(y)) continue;
                Instant ini = x.start().isAfter(y.start()) ? x.start() : y.start();
                Instant fin = x.end().isBefore(y.end()) ? x.end() : y.end();
                if (fin.isAfter(ini)) out.add(new TimeRange(ini, fin));
            }
        }
        return merge(out);
    }

    @Override
    public String toString() {
        return start + "→" + end;
    }
}
