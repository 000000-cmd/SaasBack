package com.saas.system.infrastructure.catalog;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.service.BaseCatalogService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Registro dinamico de servicios de catalogo.
 *
 * <p>Spring inyecta automaticamente todos los beans que extienden
 * {@link BaseCatalogService} y los indexa por {@code getCatalogPath()}.
 * El {@code CatalogController} usa este registry para resolver a que
 * servicio enrutar cada peticion.</p>
 *
 * <p>Agregar un catalogo nuevo solo requiere crear sus 6 clases tecnicas
 * (Entity, Domain, JpaRepo, Adapter, Mapper, Service) — el registry lo
 * detecta automaticamente en startup.</p>
 *
 * <p>Convencion: todos los catalogos del sistema usan {@link UUID} como id.
 * Esto permite que {@code get(...)} devuelva un tipo consumible por el
 * controller sin captures de wildcard.</p>
 */
@Component
public class CatalogRegistry {

    private final Map<String, BaseCatalogService<?, UUID>> registry;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public CatalogRegistry(List<BaseCatalogService> services) {
        this.registry = services.stream()
                .collect(Collectors.toUnmodifiableMap(
                        BaseCatalogService::getCatalogPath,
                        s -> (BaseCatalogService<?, UUID>) s
                ));
    }

    /**
     * @throws ResourceNotFoundException si el catalogo no existe
     */
    public BaseCatalogService<?, UUID> get(String catalogPath) {
        BaseCatalogService<?, UUID> service = registry.get(catalogPath);
        if (service == null) {
            throw new ResourceNotFoundException("Catalogo", "nombre", catalogPath);
        }
        return service;
    }

    /** Lista los paths de catalogo registrados (util para diagnostico/discovery). */
    public List<String> listAvailable() {
        return registry.keySet().stream().sorted().toList();
    }
}
