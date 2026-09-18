package com.saas.system.application.service.flow;

import com.saas.common.cqrs.ReadModelConfig;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.application.dto.request.flow.FlowControlRequest;
import com.saas.system.application.dto.request.flow.FlowFieldRequest;
import com.saas.system.application.dto.request.flow.FlowRequest;
import com.saas.system.application.dto.request.flow.FlowSectionRequest;
import com.saas.system.application.dto.response.flow.FlowAdminView;
import com.saas.system.domain.model.flow.Flow;
import com.saas.system.domain.model.flow.FlowControl;
import com.saas.system.domain.model.flow.FlowControlAction;
import com.saas.system.domain.model.flow.FlowField;
import com.saas.system.domain.model.flow.FlowSection;
import com.saas.system.domain.model.flow.FlowSectionKind;
import com.saas.system.domain.port.out.flow.IFlowControlRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowFieldRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowRepositoryPort;
import com.saas.system.domain.port.out.flow.IFlowSectionRepositoryPort;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowControlRepository;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowFieldRepository;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowRepository;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * La pestana de Flujos: crear y editar el arbol completo.
 *
 * <h3>Que se puede tocar de un flujo del sistema y que no</h3>
 * <p>El CODIGO no, y borrarlo tampoco. Hay codigo que invoca al flujo AGEND y a
 * sus secciones por su codigo: renombrarlo desde una pantalla no lo
 * reconfigura, lo rompe en silencio, y el fallo aparece mucho despues en la
 * pantalla de reservas.</p>
 *
 * <p>Todo lo demas SI: la etiqueta, el orden, los canales, la mascara, si un
 * campo es obligatorio, si un boton aparece. Eso es lo que hace configurable a
 * un flujo, y es exactamente lo que no rompe nada.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowAdminService {

    private final IFlowRepositoryPort flows;
    private final IFlowSectionRepositoryPort sections;
    private final IFlowFieldRepositoryPort fields;
    private final IFlowControlRepositoryPort controls;

    // Para reactivar lo borrado. Las claves unicas no miran Visible, asi que
    // volver a crear algo con un codigo que ya se uso choca contra el indice
    // aunque por JPA la fila no se vea. Ver los metodos revive().
    private final JpaFlowRepository jpaFlows;
    private final JpaFlowSectionRepository jpaSections;
    private final JpaFlowFieldRepository jpaFields;
    private final JpaFlowControlRepository jpaControls;

    // -----------------------------------------------------------------
    // Lectura
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FlowAdminView.Summary> list() {
        return flows.findAllOrdered().stream()
                .map(f -> new FlowAdminView.Summary(
                        f.getId(), f.getCode(), f.getName(), f.getDescription(),
                        Boolean.TRUE.equals(f.getIsSystem()), !Boolean.FALSE.equals(f.getEnabled()),
                        sections.findByFlow(f.getId()).size()))
                .toList();
    }

    /** El arbol entero, en tres consultas: secciones, campos y controles. */
    @Transactional(readOnly = true)
    public FlowAdminView tree(String code) {
        Flow flujo = flows.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "código", code));

        List<FlowSection> secciones = sections.findByFlow(flujo.getId());
        List<UUID> ids = secciones.stream().map(FlowSection::getId).toList();

        Map<UUID, List<FlowField>> camposPorSeccion = fields.findBySections(ids).stream()
                .collect(Collectors.groupingBy(FlowField::getSectionId));
        Map<UUID, List<FlowControl>> controlesPorSeccion = controls.findBySections(ids).stream()
                .collect(Collectors.groupingBy(FlowControl::getSectionId));

        List<FlowAdminView.Section> vista = new ArrayList<>();
        for (FlowSection s : secciones) {
            vista.add(new FlowAdminView.Section(
                    s.getId(), s.getCode(), s.getName(), s.getDescription(),
                    s.getDisplayOrder() == null ? 0 : s.getDisplayOrder(),
                    s.getKind() == null ? "FORM" : s.getKind().name(),
                    s.getChannels(),
                    Boolean.TRUE.equals(s.getIsSystem()), !Boolean.FALSE.equals(s.getEnabled()),
                    camposPorSeccion.getOrDefault(s.getId(), List.of()).stream()
                            .map(FlowAdminService::toView).toList(),
                    controlesPorSeccion.getOrDefault(s.getId(), List.of()).stream()
                            .map(FlowAdminService::toView).toList()));
        }

        return new FlowAdminView(flujo.getId(), flujo.getCode(), flujo.getName(),
                flujo.getDescription(), Boolean.TRUE.equals(flujo.getIsSystem()),
                !Boolean.FALSE.equals(flujo.getEnabled()), vista);
    }

    // -----------------------------------------------------------------
    // Flujo
    // -----------------------------------------------------------------

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView createFlow(FlowRequest req) {
        if (flows.existsByCode(req.code())) {
            throw new BusinessException("Ya existe un flujo con el código " + req.code());
        }
        // Si el codigo existio y se borro, vuelve con lo suyo en vez de
        // estrellarse contra el indice unico.
        if (jpaFlows.reviveByCode(req.code(), req.name(), req.description()) > 0) {
            return tree(req.code());
        }
        Flow f = Flow.builder()
                .code(req.code()).name(req.name()).description(req.description())
                .isSystem(false)
                .build();
        if (req.enabled() != null) f.setEnabled(req.enabled());
        flows.save(f);
        return tree(req.code());
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView updateFlow(UUID id, FlowRequest req) {
        Flow f = flows.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "Id", id));
        if (!f.getCode().equals(req.code())) {
            exigirNoDeSistema(f.getIsSystem(),
                    "El código de un flujo del sistema no se puede cambiar: hay pantallas que lo invocan por él");
            if (flows.existsByCode(req.code())) {
                throw new BusinessException("Ya existe un flujo con el código " + req.code());
            }
            f.setCode(req.code());
        }
        f.setName(req.name());
        f.setDescription(req.description());
        if (req.enabled() != null) f.setEnabled(req.enabled());
        flows.update(f);
        return tree(f.getCode());
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public void deleteFlow(UUID id) {
        Flow f = flows.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "Id", id));
        exigirNoDeSistema(f.getIsSystem(),
                "Los flujos del sistema no se borran. Desactívalo si no quieres usarlo");
        flows.softDeleteById(id);
    }

    // -----------------------------------------------------------------
    // Seccion
    // -----------------------------------------------------------------

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView createSection(UUID flowId, FlowSectionRequest req) {
        Flow f = flows.findById(flowId)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "Id", flowId));
        if (sections.findByFlowAndCode(flowId, req.code()).isPresent()) {
            throw new BusinessException("Ese flujo ya tiene una sección con el código " + req.code());
        }
        if (jpaSections.revive(flowId.toString(), req.code(), req.name()) > 0) {
            return tree(f.getCode());
        }
        FlowSection s = FlowSection.builder()
                .flowId(flowId).code(req.code()).name(req.name()).description(req.description())
                .displayOrder(req.displayOrder() == null ? siguienteOrden(flowId) : req.displayOrder())
                .kind(FlowSectionKind.of(req.kind()))
                .channels(canales(req.channels()))
                .isSystem(false)
                .build();
        if (req.enabled() != null) s.setEnabled(req.enabled());
        sections.save(s);
        return tree(f.getCode());
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView updateSection(UUID id, FlowSectionRequest req) {
        FlowSection s = sections.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección de flujo", "Id", id));
        if (!s.getCode().equals(req.code())) {
            exigirNoDeSistema(s.getIsSystem(),
                    "El código de una sección del sistema no se puede cambiar: el flujo la busca por él");
            s.setCode(req.code());
        }
        s.setName(req.name());
        s.setDescription(req.description());
        if (req.displayOrder() != null) s.setDisplayOrder(req.displayOrder());
        if (req.kind() != null) s.setKind(FlowSectionKind.of(req.kind()));
        if (req.channels() != null) s.setChannels(canales(req.channels()));
        if (req.enabled() != null) s.setEnabled(req.enabled());
        sections.update(s);
        return treeOfSection(s);
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView deleteSection(UUID id) {
        FlowSection s = sections.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección de flujo", "Id", id));
        exigirNoDeSistema(s.getIsSystem(),
                "Esa sección es parte del flujo del sistema. Desactívala en vez de borrarla");
        // Los hijos se van con ella: dejarlos sueltos llenaria la tabla de
        // campos que ya no pinta nadie y que reaparecerian si el codigo se
        // vuelve a usar.
        for (FlowField c : fields.findBySection(id)) fields.softDeleteById(c.getId());
        for (FlowControl c : controls.findBySection(id)) controls.softDeleteById(c.getId());
        sections.softDeleteById(id);
        return treeOfSection(s);
    }

    // -----------------------------------------------------------------
    // Campo
    // -----------------------------------------------------------------

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView createField(UUID sectionId, FlowFieldRequest req) {
        FlowSection s = sections.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sección de flujo", "Id", sectionId));
        boolean repetido = fields.findBySection(sectionId).stream()
                .anyMatch(f -> f.getCode().equalsIgnoreCase(req.code()));
        if (repetido) {
            throw new BusinessException("Esa sección ya tiene un campo con el código " + req.code());
        }
        if (jpaFields.revive(sectionId.toString(), req.code(), req.label()) > 0) {
            return treeOfSection(s);
        }
        FlowField f = FlowField.builder()
                .sectionId(sectionId).code(req.code()).label(req.label())
                .placeholder(req.placeholder()).helpText(req.helpText())
                .dataType(req.dataType() == null ? "text" : req.dataType())
                .maskCode(req.maskCode()).sourceKey(req.sourceKey())
                .isRequired(Boolean.TRUE.equals(req.isRequired()))
                .width(req.width() == null ? "full" : req.width())
                .displayOrder(req.displayOrder() == null ? 0 : req.displayOrder())
                .isSystem(false)
                .build();
        if (req.enabled() != null) f.setEnabled(req.enabled());
        fields.save(f);
        return treeOfSection(s);
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView updateField(UUID id, FlowFieldRequest req) {
        FlowField f = fields.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campo de flujo", "Id", id));
        if (!f.getCode().equals(req.code())) {
            exigirNoDeSistema(f.getIsSystem(),
                    "El código de un campo del sistema no se puede cambiar: es la clave con la que el dato viaja");
            f.setCode(req.code());
        }
        f.setLabel(req.label());
        f.setPlaceholder(req.placeholder());
        f.setHelpText(req.helpText());
        if (req.dataType() != null) f.setDataType(req.dataType());
        f.setMaskCode(req.maskCode());
        f.setSourceKey(req.sourceKey());
        if (req.isRequired() != null) f.setIsRequired(req.isRequired());
        if (req.width() != null) f.setWidth(req.width());
        if (req.displayOrder() != null) f.setDisplayOrder(req.displayOrder());
        if (req.enabled() != null) f.setEnabled(req.enabled());
        fields.update(f);
        return treeOfSectionId(f.getSectionId());
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView deleteField(UUID id) {
        FlowField f = fields.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campo de flujo", "Id", id));
        exigirNoDeSistema(f.getIsSystem(),
                "Ese campo es parte del flujo del sistema. Desactívalo en vez de borrarlo");
        fields.softDeleteById(id);
        return treeOfSectionId(f.getSectionId());
    }

    // -----------------------------------------------------------------
    // Control
    // -----------------------------------------------------------------

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView createControl(UUID sectionId, FlowControlRequest req) {
        FlowSection s = sections.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sección de flujo", "Id", sectionId));
        boolean repetido = controls.findBySection(sectionId).stream()
                .anyMatch(c -> c.getCode().equalsIgnoreCase(req.code()));
        if (repetido) {
            throw new BusinessException("Esa sección ya tiene un control con el código " + req.code());
        }
        if (jpaControls.revive(sectionId.toString(), req.code(), req.label()) > 0) {
            return treeOfSection(s);
        }
        FlowControl c = FlowControl.builder()
                .sectionId(sectionId).code(req.code()).label(req.label())
                .action(FlowControlAction.of(req.action()))
                .variant(req.variant() == null ? "primary" : req.variant())
                .permissionCode(req.permissionCode())
                .channels(canales(req.channels()))
                .displayOrder(req.displayOrder() == null ? 0 : req.displayOrder())
                .isSystem(false)
                .build();
        if (req.enabled() != null) c.setEnabled(req.enabled());
        controls.save(c);
        return treeOfSection(s);
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView updateControl(UUID id, FlowControlRequest req) {
        FlowControl c = controls.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control de flujo", "Id", id));
        if (!c.getCode().equals(req.code())) {
            exigirNoDeSistema(c.getIsSystem(),
                    "El código de un control del sistema no se puede cambiar: la pantalla lo busca por él");
            c.setCode(req.code());
        }
        c.setLabel(req.label());
        if (req.action() != null) c.setAction(FlowControlAction.of(req.action()));
        if (req.variant() != null) c.setVariant(req.variant());
        c.setPermissionCode(req.permissionCode());
        if (req.channels() != null) c.setChannels(canales(req.channels()));
        if (req.displayOrder() != null) c.setDisplayOrder(req.displayOrder());
        if (req.enabled() != null) c.setEnabled(req.enabled());
        controls.update(c);
        return treeOfSectionId(c.getSectionId());
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowAdminView deleteControl(UUID id) {
        FlowControl c = controls.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control de flujo", "Id", id));
        exigirNoDeSistema(c.getIsSystem(),
                "Ese control es parte del flujo del sistema. Desactívalo en vez de borrarlo");
        controls.softDeleteById(id);
        return treeOfSectionId(c.getSectionId());
    }

    // -----------------------------------------------------------------
    // Apoyo
    // -----------------------------------------------------------------

    private void exigirNoDeSistema(Boolean esDelSistema, String mensaje) {
        if (Boolean.TRUE.equals(esDelSistema)) throw new BusinessException(mensaje);
    }

    /** Al final de la lista: una seccion nueva no se cuela en medio del flujo. */
    private int siguienteOrden(UUID flowId) {
        return sections.findByFlow(flowId).stream()
                .map(FlowSection::getDisplayOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
    }

    /** Normaliza el CSV de canales. Vacio = todos, que es lo mismo que nada. */
    private String canales(String csv) {
        if (csv == null || csv.isBlank()) return "WEB,PANEL,APK,WHATSAPP";
        return csv.toUpperCase().replace(" ", "");
    }

    private FlowAdminView treeOfSection(FlowSection s) {
        return flows.findById(s.getFlowId()).map(f -> tree(f.getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Flujo", "Id", s.getFlowId()));
    }

    private FlowAdminView treeOfSectionId(UUID sectionId) {
        return sections.findById(sectionId).map(this::treeOfSection)
                .orElseThrow(() -> new ResourceNotFoundException("Sección de flujo", "Id", sectionId));
    }

    private static FlowAdminView.Field toView(FlowField f) {
        return new FlowAdminView.Field(
                f.getId(), f.getCode(), f.getLabel(), f.getPlaceholder(), f.getHelpText(),
                f.getDataType(), f.getMaskCode(), f.getSourceKey(),
                Boolean.TRUE.equals(f.getIsRequired()), f.getWidth(),
                f.getDisplayOrder() == null ? 0 : f.getDisplayOrder(),
                Boolean.TRUE.equals(f.getIsSystem()), !Boolean.FALSE.equals(f.getEnabled()));
    }

    private static FlowAdminView.Control toView(FlowControl c) {
        return new FlowAdminView.Control(
                c.getId(), c.getCode(), c.getLabel(),
                c.getAction() == null ? "CUSTOM" : c.getAction().name(),
                c.getVariant(), c.getPermissionCode(), c.getChannels(),
                c.getDisplayOrder() == null ? 0 : c.getDisplayOrder(),
                Boolean.TRUE.equals(c.getIsSystem()), !Boolean.FALSE.equals(c.getEnabled()));
    }
}
