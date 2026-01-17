package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.DynamicList;
import com.saas.system.domain.model.ListDefinition;
import com.saas.system.domain.port.in.IDynamicListUseCase;
import com.saas.system.domain.port.out.IDynamicListRepositoryPort;
import com.saas.system.domain.port.out.IListDefinitionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de listas dinámicas.
 * Implementa la lógica de negocio para CRUD sobre cualquier lista del sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicListService implements IDynamicListUseCase {

    private final IDynamicListRepositoryPort dynamicListRepository;
    private final IListDefinitionRepositoryPort listDefinitionRepository;

    // ==================== Operaciones sobre definiciones de listas ====================

    @Override
    @Transactional(readOnly = true)
    public List<ListDefinition> getAllListDefinitions() {
        log.debug("Obteniendo todas las definiciones de listas");
        return listDefinitionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ListDefinition getListDefinitionByTableName(String physicalTableName) {
        log.debug("Buscando definición de lista por tabla: {}", physicalTableName);
        return listDefinitionRepository.findByPhysicalTableName(physicalTableName)
                .orElseThrow(() -> new ResourceNotFoundException("Definición de lista", "tabla", physicalTableName));
    }

    @Override
    @Transactional
    public ListDefinition createListDefinition(ListDefinition definition) {
        log.debug("Creando definición de lista: {}", definition.getDisplayName());

        // Validar que no exista otra definición con el mismo nombre de tabla
        if (listDefinitionRepository.existsByPhysicalTableName(definition.getPhysicalTableName())) {
            throw new BusinessException(
                    String.format("Ya existe una definición para la tabla '%s'", definition.getPhysicalTableName()));
        }

        definition.markAsCreated("SYSTEM");
        ListDefinition saved = listDefinitionRepository.save(definition);
        log.info("Definición de lista creada: {} (ID: {})", saved.getDisplayName(), saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public ListDefinition updateListDefinition(String id, ListDefinition definition) {
        log.debug("Actualizando definición de lista ID: {}", id);

        ListDefinition existing = listDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Definición de lista", "ID", id));

        existing.setDisplayName(definition.getDisplayName());
        existing.setPhysicalTableName(definition.getPhysicalTableName());
        existing.markAsUpdated("SYSTEM");

        ListDefinition updated = listDefinitionRepository.update(existing);
        log.info("Definición de lista actualizada: {} (ID: {})", updated.getDisplayName(), id);
        return updated;
    }

    @Override
    @Transactional
    public void deleteListDefinition(String id) {
        log.debug("Eliminando definición de lista ID: {}", id);

        if (!listDefinitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Definición de lista", "ID", id);
        }

        listDefinitionRepository.deleteById(id);
        log.info("Definición de lista eliminada ID: {}", id);
    }

    // ==================== Operaciones sobre items de listas ====================

    @Override
    @Transactional(readOnly = true)
    public List<DynamicList> getAllItems(String listType) {
        log.debug("Obteniendo todos los items de la lista: {}", listType);
        String tableName = resolveTableName(listType);
        return dynamicListRepository.findAllVisible(tableName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicList> getEnabledItems(String listType) {
        log.debug("Obteniendo items habilitados de la lista: {}", listType);
        String tableName = resolveTableName(listType);
        return dynamicListRepository.findAllEnabled(tableName);
    }

    @Override
    @Transactional(readOnly = true)
    public DynamicList getItemById(String listType, String id) {
        log.debug("Buscando item por ID {} en lista: {}", id, listType);
        String tableName = resolveTableName(listType);
        return dynamicListRepository.findById(tableName, id)
                .orElseThrow(() -> new ResourceNotFoundException(listType, "ID", id));
    }

    @Override
    @Transactional(readOnly = true)
    public DynamicList getItemByCode(String listType, String code) {
        log.debug("Buscando item por código {} en lista: {}", code, listType);
        String tableName = resolveTableName(listType);
        return dynamicListRepository.findByCode(tableName, code)
                .orElseThrow(() -> new ResourceNotFoundException(listType, "código", code));
    }

    @Override
    @Transactional
    public DynamicList createItem(String listType, DynamicList item) {
        log.debug("Creando item en lista {}: {}", listType, item.getCode());
        String tableName = resolveTableName(listType);

        // Validar que no exista otro item con el mismo código
        if (dynamicListRepository.existsByCode(tableName, item.getCode())) {
            throw new BusinessException(
                    String.format("Ya existe un item con código '%s' en la lista '%s'", item.getCode(), listType));
        }

        item.setListType(listType);
        item.markAsCreated("SYSTEM");

        DynamicList saved = dynamicListRepository.save(tableName, item);
        log.info("Item creado en lista {}: {} (ID: {})", listType, saved.getCode(), saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public DynamicList updateItem(String listType, String id, DynamicList item) {
        log.debug("Actualizando item {} en lista: {}", id, listType);
        String tableName = resolveTableName(listType);

        DynamicList existing = dynamicListRepository.findById(tableName, id)
                .orElseThrow(() -> new ResourceNotFoundException(listType, "ID", id));

        // Si el código cambió, verificar que no exista otro item con ese código
        if (!existing.getCode().equals(item.getCode()) &&
                dynamicListRepository.existsByCode(tableName, item.getCode())) {
            throw new BusinessException(
                    String.format("Ya existe un item con código '%s' en la lista '%s'", item.getCode(), listType));
        }

        existing.setCode(item.getCode());
        existing.setName(item.getName());
        existing.setDisplayOrder(item.getDisplayOrder());
        existing.markAsUpdated("SYSTEM");

        DynamicList updated = dynamicListRepository.update(tableName, existing);
        log.info("Item actualizado en lista {}: {} (ID: {})", listType, updated.getCode(), id);
        return updated;
    }

    @Override
    @Transactional
    public void toggleItemEnabled(String listType, String id, boolean enabled) {
        log.debug("Cambiando estado de item {} en lista {} a: {}", id, listType, enabled);
        String tableName = resolveTableName(listType);

        if (!dynamicListRepository.existsById(tableName, id)) {
            throw new ResourceNotFoundException(listType, "ID", id);
        }

        dynamicListRepository.toggleEnabled(tableName, id, enabled);
        log.info("Estado de item {} en lista {} cambiado a: {}", id, listType, enabled);
    }

    @Override
    @Transactional
    public void deleteItem(String listType, String id) {
        log.debug("Eliminando item {} de lista: {}", id, listType);
        String tableName = resolveTableName(listType);

        if (!dynamicListRepository.existsById(tableName, id)) {
            throw new ResourceNotFoundException(listType, "ID", id);
        }

        dynamicListRepository.softDelete(tableName, id);
        log.info("Item {} eliminado de lista: {}", id, listType);
    }

    // ==================== Métodos privados ====================

    /**
     * Resuelve el nombre de la tabla física a partir del identificador de lista.
     * Valida que la lista exista en las definiciones.
     *
     * @param listType Identificador de la lista (ej: "document-types", "gender-types")
     * @return Nombre de la tabla física
     */
    private String resolveTableName(String listType) {
        // Normalizar: convertir guiones a guiones bajos y agregar prefijo
        String normalizedType = listType.replace("-", "_").toLowerCase();
        String tableName = "sys_list_" + normalizedType;

        // Verificar que la tabla exista
        if (!dynamicListRepository.tableExists(tableName)) {
            // Intentar buscar en las definiciones
            ListDefinition definition = listDefinitionRepository.findByPhysicalTableName(tableName)
                    .orElseThrow(() -> new ResourceNotFoundException("Lista", "tipo", listType));
            tableName = definition.getPhysicalTableName();
        }

        return tableName;
    }
}