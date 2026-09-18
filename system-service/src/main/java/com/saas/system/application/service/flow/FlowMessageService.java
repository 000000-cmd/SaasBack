package com.saas.system.application.service.flow;

import com.saas.common.cqrs.ReadModelConfig;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.application.dto.request.flow.FlowMessageRequest;
import com.saas.system.application.dto.response.flow.FlowMessageView;
import com.saas.system.domain.model.flow.FlowMessage;
import com.saas.system.domain.port.out.flow.IFlowMessageRepositoryPort;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Los textos de los flujos, configurables y consumidos POR CODIGO.
 *
 * <p>No son plantillas de notificacion: aquellas tienen destinatario, canal y
 * parametros. Estas son lo que la pantalla o el bot dicen en un paso, y por eso
 * no cuelgan de una seccion ni de un control — el mismo texto aparece en varios
 * sitios y atarlo a uno obligaria a duplicarlo.</p>
 */
@Service
@RequiredArgsConstructor
public class FlowMessageService {

    private final IFlowMessageRepositoryPort repo;
    /** Para reactivar un texto borrado: la clave unica del codigo no mira Visible. */
    private final JpaFlowMessageRepository jpa;

    @Transactional(readOnly = true)
    public List<FlowMessageView> list() {
        return repo.findAllOrdered().stream().map(FlowMessageService::toView).toList();
    }

    /**
     * Todos los textos activos, del codigo al cuerpo.
     *
     * <p>De golpe y en cache: una pantalla necesita los suyos y pedirlos de uno
     * en uno serian diez viajes para diez frases que cambian una vez al ano.</p>
     */
    @Cacheable(value = ReadModelConfig.CATALOGS, key = "'flow:messages'")
    @Transactional(readOnly = true)
    public Map<String, String> byCode() {
        return repo.findAllOrdered().stream()
                .filter(m -> !Boolean.FALSE.equals(m.getEnabled()))
                .collect(Collectors.toMap(FlowMessage::getCode, FlowMessage::getBody,
                        (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public FlowMessageView getByCode(String code) {
        return repo.findByCode(code)
                .map(FlowMessageService::toView)
                .orElseThrow(() -> new ResourceNotFoundException("Texto de flujo", "código", code));
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowMessageView create(FlowMessageRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new BusinessException("Ya existe un texto con el código " + req.code());
        }
        if (jpa.reviveByCode(req.code(), req.name(), req.body(), req.description()) > 0) {
            return getByCode(req.code());
        }
        FlowMessage m = FlowMessage.builder()
                .code(req.code()).name(req.name()).body(req.body())
                .description(req.description())
                .build();
        if (req.enabled() != null) m.setEnabled(req.enabled());
        return toView(repo.save(m));
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public FlowMessageView update(UUID id, FlowMessageRequest req) {
        FlowMessage m = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Texto de flujo", "Id", id));
        if (!m.getCode().equals(req.code())) {
            if (repo.existsByCode(req.code())) {
                throw new BusinessException("Ya existe un texto con el código " + req.code());
            }
            m.setCode(req.code());
        }
        m.setName(req.name());
        m.setBody(req.body());
        m.setDescription(req.description());
        if (req.enabled() != null) m.setEnabled(req.enabled());
        return toView(repo.update(m));
    }

    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, allEntries = true)
    public void delete(UUID id) {
        if (repo.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Texto de flujo", "Id", id);
        }
        repo.softDeleteById(id);
    }

    private static FlowMessageView toView(FlowMessage m) {
        return new FlowMessageView(m.getId(), m.getCode(), m.getName(), m.getBody(),
                m.getDescription(), !Boolean.FALSE.equals(m.getEnabled()));
    }
}
