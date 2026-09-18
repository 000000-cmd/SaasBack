package com.saas.business.application.service;

import com.saas.business.infrastructure.persistence.entity.DayOfWeekRefEntity;
import com.saas.business.infrastructure.persistence.repository.JpaDayOfWeekRefRepository;
import com.saas.common.cqrs.ReadModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El catalogo de dias de la semana: del id de la fila al numero ISO.
 *
 * <p>Siete filas que no cambian nunca y que hacen falta en CADA calculo de
 * disponibilidad. En cache, con clave fija: el catalogo es global, no por
 * negocio.</p>
 *
 * <p>Es un componente aparte y no un metodo del servicio de disponibilidad
 * porque {@code @Cacheable} solo funciona cuando la llamada cruza el proxy de
 * Spring. Llamandose a si mismo, la anotacion no hace nada y la consulta se
 * repetiria en cada peticion sin que nadie lo note.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayOfWeekCatalog {

    private final JpaDayOfWeekRefRepository repo;

    @Cacheable(value = ReadModelConfig.CATALOGS, key = "'business:dayOfWeekIso'")
    public Map<UUID, DayOfWeek> isoById() {
        Map<UUID, DayOfWeek> out = new HashMap<>();
        for (DayOfWeekRefEntity d : repo.findAll()) {
            try {
                out.put(d.getId(), DayOfWeek.of(Integer.parseInt(d.getValue().trim())));
            } catch (RuntimeException ex) {
                log.warn("Día de la semana con valor ilegible: {} = {}", d.getCode(), d.getValue());
            }
        }
        return out;
    }
}
