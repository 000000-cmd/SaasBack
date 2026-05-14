package com.saas.common.service;

import com.saas.common.model.BaseCatalogDomain;
import com.saas.common.port.in.ICatalogUseCase;
import com.saas.common.port.out.ICatalogRepositoryPort;

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
}
