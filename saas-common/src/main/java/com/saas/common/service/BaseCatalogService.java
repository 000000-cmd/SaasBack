package com.saas.common.service;

import com.saas.common.cqrs.ReadModelConfig;
import com.saas.common.model.BaseCatalogDomain;
import com.saas.common.port.in.ICatalogUseCase;
import com.saas.common.port.out.ICatalogRepositoryPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio CRUD base para catalogos.
 *
 * Implementa {@code applyChanges} de una vez para los 4 campos comunes
 * (code, name, value, displayOrder). Las subclases concretas SOLO deben
 * implementar {@link #getResourceName()} y {@link #getCatalogPath()}.
 *
 * El {@code catalogPath} es la clave publica del catalogo (ej. "document_types")
 * usada por {@code CatalogController} para enrutar dinamicamente las peticiones.
 */
public abstract class BaseCatalogService<T extends BaseCatalogDomain, ID>
        extends CodeCrudService<T, ID>
        implements ICatalogUseCase<T, ID> {

    protected BaseCatalogService(ICatalogRepositoryPort<T, ID> repository) {
        super(repository);
    }

    /**
     * Nombre publico del catalogo, usado en la URL: {@code /list/{catalogPath}}.
     * Convencion: snake_case en plural (ej. "document_types", "registration_statuses").
     */
    public abstract String getCatalogPath();

    /**
     * Crea una instancia vacia del dominio concreto de este catalogo.
     * Necesario para que el controller generico pueda construir objetos
     * sin conocer la clase concreta en tiempo de compilacion.
     */
    public abstract T newInstance();

    @Override
    protected void applyChanges(T existing, T incoming) {
        if (incoming.getCode() != null)         existing.setCode(incoming.getCode());
        if (incoming.getName() != null)         existing.setName(incoming.getName());
        if (incoming.getValue() != null)        existing.setValue(incoming.getValue());
        if (incoming.getDisplayOrder() != null) existing.setDisplayOrder(incoming.getDisplayOrder());
    }

    // -----------------------------------------------------------------
    // Lado de lectura (ver ReadModelConfig)
    // -----------------------------------------------------------------
    // Un catalogo se lee en casi cada pantalla y se escribe una vez cada
    // varios meses. Cachear la lista completa aqui — y no en cada uno de los
    // catalogos concretos — es el unico sitio donde hace falta escribirlo:
    // todos heredan de esta clase.
    //
    // La clave es la ruta publica del catalogo, que ya es su identidad
    // ("gender", "document_type"...). Sin ella, todos compartirian una sola
    // entrada y el ultimo en leer le devolveria sus filas al siguiente.

    /**
     * Devuelve una copia inmutable: lo que sale de aqui lo comparten todos los
     * que llamen despues, y una lista que alguien pueda ordenar o filtrar en
     * sitio dejaria el cache alterado para el resto.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ReadModelConfig.CATALOGS, key = "#root.target.catalogPath")
    public List<T> getAll() {
        return List.copyOf(super.getAll());
    }

    @Override
    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, key = "#root.target.catalogPath")
    public T create(T entity) {
        return super.create(entity);
    }

    @Override
    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, key = "#root.target.catalogPath")
    public T update(ID id, T incoming) {
        return super.update(id, incoming);
    }

    @Override
    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, key = "#root.target.catalogPath")
    public void delete(ID id) {
        super.delete(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = ReadModelConfig.CATALOGS, key = "#root.target.catalogPath")
    public void toggleEnabled(ID id, boolean enabled) {
        super.toggleEnabled(id, enabled);
    }
}
