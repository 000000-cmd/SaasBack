package com.saas.system.domain.port.in;

import com.saas.system.domain.model.DynamicList;
import com.saas.system.domain.model.ListDefinition;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para gestión de listas dinámicas.
 * Permite CRUD genérico sobre cualquier lista definida en el sistema.
 */
public interface IDynamicListUseCase {

    // ==================== Operaciones sobre definiciones de listas ====================

    /**
     * Obtiene todas las definiciones de listas disponibles en el sistema.
     * @return Lista de definiciones de listas
     */
    List<ListDefinition> getAllListDefinitions();

    /**
     * Obtiene una definición de lista por su nombre de tabla física.
     * @param physicalTableName Nombre de la tabla física
     * @return Definición de la lista
     */
    ListDefinition getListDefinitionByTableName(String physicalTableName);

    /**
     * Crea una nueva definición de lista.
     * @param definition Definición a crear
     * @return Definición creada
     */
    ListDefinition createListDefinition(ListDefinition definition);

    /**
     * Actualiza una definición de lista existente.
     * @param id ID de la definición
     * @param definition Datos actualizados
     * @return Definición actualizada
     */
    ListDefinition updateListDefinition(String id, ListDefinition definition);

    /**
     * Elimina una definición de lista (soft delete).
     * @param id ID de la definición
     */
    void deleteListDefinition(String id);

    // ==================== Operaciones sobre items de listas ====================

    /**
     * Obtiene todos los items de una lista específica.
     * @param listType Identificador de la lista (nombre de tabla física sin prefijo)
     * @return Lista de items
     */
    List<DynamicList> getAllItems(String listType);

    /**
     * Obtiene solo los items habilitados de una lista.
     * @param listType Identificador de la lista
     * @return Lista de items habilitados
     */
    List<DynamicList> getEnabledItems(String listType);

    /**
     * Obtiene un item por su ID.
     * @param listType Identificador de la lista
     * @param id ID del item
     * @return Item encontrado
     */
    DynamicList getItemById(String listType, String id);

    /**
     * Obtiene un item por su código.
     * @param listType Identificador de la lista
     * @param code Código del item
     * @return Item encontrado
     */
    DynamicList getItemByCode(String listType, String code);

    /**
     * Crea un nuevo item en una lista.
     * @param listType Identificador de la lista
     * @param item Item a crear
     * @return Item creado
     */
    DynamicList createItem(String listType, DynamicList item);

    /**
     * Actualiza un item existente.
     * @param listType Identificador de la lista
     * @param id ID del item
     * @param item Datos actualizados
     * @return Item actualizado
     */
    DynamicList updateItem(String listType, String id, DynamicList item);

    /**
     * Cambia el estado habilitado de un item.
     * @param listType Identificador de la lista
     * @param id ID del item
     * @param enabled Nuevo estado
     */
    void toggleItemEnabled(String listType, String id, boolean enabled);

    /**
     * Elimina un item de una lista (soft delete).
     * @param listType Identificador de la lista
     * @param id ID del item
     */
    void deleteItem(String listType, String id);
}