package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.ListDefinitionRequest;
import com.saas.system.application.dto.response.ListDefinitionResponse;
import com.saas.system.domain.model.ListDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Mapper para conversión de ListDefinition entre capas.
 */
@Mapper(componentModel = "spring")
public interface ListDefinitionMapper extends
        IRequestMapper<ListDefinition, ListDefinitionRequest>,
        IResponseMapper<ListDefinition, ListDefinitionResponse> {

    @Override
    ListDefinition toDomain(ListDefinitionRequest request);

    @Override
    @Mapping(target = "listType", source = "physicalTableName", qualifiedByName = "toListType")
    ListDefinitionResponse toResponse(ListDefinition domain);

    /**
     * Convierte el nombre de tabla física a un identificador amigable.
     * Ejemplo: sys_list_document_types -> document-types
     */
    @Named("toListType")
    default String toListType(String physicalTableName) {
        if (physicalTableName == null) {
            return null;
        }
        // Remover prefijo "sys_list_" y convertir guiones bajos a guiones
        String type = physicalTableName.replace("sys_list_", "");
        return type.replace("_", "-");
    }
}