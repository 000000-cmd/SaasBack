package com.saas.authservice.mappers;

import com.saas.authservice.dto.request.ListTypeRequestDTO;
import com.saas.authservice.dto.response.ListTypeResponseDTO;
import com.saas.authservice.entities.ListRole;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


/**
 * Interfaz de Mapper que utiliza MapStruct para convertir entre DTOs y Entidades.
 * Centraliza toda la lógica de mapeo para las listas.
 */
@Mapper(componentModel = "spring")
public interface ListMapper {

    ListTypeResponseDTO toResponseDTO(ListRole entity);
    ListRole toRoleTypeEntity(ListTypeRequestDTO dto);
    void updateRoleTypeFromDto(ListTypeRequestDTO dto, @MappingTarget ListRole entity);

}

