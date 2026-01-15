package com.saas.authservice.services;


import com.saas.authservice.dto.request.ListTypeRequestDTO;
import com.saas.authservice.dto.response.ListTypeResponseDTO;
import com.saas.authservice.exceptions.ResourceNotFoundException;
import com.saas.authservice.mappers.ListMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Clase abstracta que contiene la lógica CRUD genérica para todas las entidades de tipo lista.
 * @param <E> La clase de la Entidad (ej. ListCategory)
 * @param <R> El Repositorio para la entidad (ej. ListCategoryRepository)
 * @param <T> La Entidad base de la lista que tiene setAuditDate (ej. ListCategory)
 */
public abstract class AbstractListService<E, T extends E, R extends JpaRepository<E, UUID>> {

    protected final R repository;
    protected final ListMapper mapper;
    protected final String entityName;

    public AbstractListService(R repository, ListMapper mapper, String entityName) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityName = entityName;
    }

    // Métodos abstractos que cada servicio hijo debe implementar
    protected abstract Function<ListTypeRequestDTO, T> toEntity();
    protected abstract Function<E, ListTypeResponseDTO> toResponseDTO();
    protected abstract BiConsumer<ListTypeRequestDTO, T> updateFromDto();
    protected abstract void setSharedProperties(T entity); // Para setear ID, AuditDate, etc.
    protected abstract void setAuditDate(T entity, LocalDateTime date);

    @Transactional(readOnly = true)
    public List<ListTypeResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(toResponseDTO())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListTypeResponseDTO findById(UUID id) {
        E entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName + " no encontrado con ID: " + id));
        return toResponseDTO().apply(entity);
    }

    @Transactional
    public ListTypeResponseDTO create(ListTypeRequestDTO requestDTO) {
        // La validación de código duplicado se hace en el servicio específico
        T newEntity = toEntity().apply(requestDTO);
        setSharedProperties(newEntity);
        // Aquí se asignaría el usuario auditor: newEntity.setUserAudit(currentUser);

        E savedEntity = repository.save(newEntity);
        return toResponseDTO().apply(savedEntity);
    }

    @Transactional
    public ListTypeResponseDTO update(UUID id, ListTypeRequestDTO requestDTO) {
        T existingEntity = (T) repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName + " no encontrado para actualizar con ID: " + id));

        updateFromDto().accept(requestDTO, existingEntity);
        setAuditDate(existingEntity, LocalDateTime.now());
        // Aquí se actualizaría el usuario de auditoría
        // existingEntity.setUserAudit(currentUser);

        E updatedEntity = repository.save(existingEntity);
        return toResponseDTO().apply(updatedEntity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(entityName + " no encontrado para eliminar con ID: " + id);
        }
        repository.deleteById(id);
    }
}