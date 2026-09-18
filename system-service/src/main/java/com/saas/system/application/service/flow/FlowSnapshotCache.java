package com.saas.system.application.service.flow;

import com.saas.common.cqrs.ReadModelConfig;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.flow.Flow;
import com.saas.system.domain.model.flow.FlowChannel;
import com.saas.system.domain.model.flow.FlowControl;
import com.saas.system.domain.model.flow.FlowField;
import com.saas.system.domain.model.flow.FlowSection;
import com.saas.system.domain.port.out.flow.IFlowControlRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowFieldRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowSectionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * El flujo de un canal, ya filtrado por activo y por canal, EN CACHE.
 *
 * <p>Un flujo se lee en cada carga de la pantalla de reservas y cambia una vez
 * cada varios meses. Sin cache serian tres consultas por visita para devolver
 * siempre lo mismo.</p>
 *
 * <p>Lo que se guarda NO lleva el filtro de permisos: eso depende de quien
 * pregunta y se aplica despues, en {@link FlowResolverService}. Guardarlo ya
 * filtrado obligaria a una entrada de cache por combinacion de permisos.</p>
 *
 * <p>Componente aparte y no un metodo del resolver porque {@code @Cacheable}
 * solo actua cuando la llamada cruza el proxy de Spring: llamandose a si mismo,
 * la anotacion no hace nada y nadie se entera.</p>
 *
 * <p>Quien lea el resultado NO debe modificarlo: es la misma instancia que verá
 * la siguiente peticion. El resolver solo lee.</p>
 */
@Component
@RequiredArgsConstructor
public class FlowSnapshotCache {

    private final IFlowRepositoryPort flows;
    private final IFlowSectionRepositoryPort sections;
    private final IFlowFieldRepositoryPort fields;
    private final IFlowControlRepositoryPort controls;

    /** Una seccion con lo suyo, tal cual sale de la base. */
    public record SectionSnapshot(FlowSection section, List<FlowField> fields,
                                  List<FlowControl> controls) {}

    public record Snapshot(Flow flow, FlowChannel channel, List<SectionSnapshot> sections) {}

    @Cacheable(value = ReadModelConfig.CATALOGS, key = "'flow:' + #code + ':' + #channel")
    @Transactional(readOnly = true)
    public Snapshot forChannel(String code, FlowChannel channel) {
        Flow flujo = flows.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "código", code));

        List<FlowSection> vivas = sections.findByFlow(flujo.getId()).stream()
                .filter(s -> !Boolean.FALSE.equals(s.getEnabled()))
                .filter(s -> channel.inCsv(s.getChannels()))
                .toList();

        List<UUID> ids = vivas.stream().map(FlowSection::getId).toList();
        Map<UUID, List<FlowField>> camposPorSeccion = fields.findBySections(ids).stream()
                .filter(f -> !Boolean.FALSE.equals(f.getEnabled()))
                .collect(Collectors.groupingBy(FlowField::getSectionId));
        Map<UUID, List<FlowControl>> controlesPorSeccion = controls.findBySections(ids).stream()
                .filter(c -> !Boolean.FALSE.equals(c.getEnabled()))
                .filter(c -> channel.inCsv(c.getChannels()))
                .collect(Collectors.groupingBy(FlowControl::getSectionId));

        List<SectionSnapshot> out = new ArrayList<>();
        for (FlowSection s : vivas) {
            out.add(new SectionSnapshot(s,
                    camposPorSeccion.getOrDefault(s.getId(), List.of()),
                    controlesPorSeccion.getOrDefault(s.getId(), List.of())));
        }
        return new Snapshot(flujo, channel, out);
    }
}
