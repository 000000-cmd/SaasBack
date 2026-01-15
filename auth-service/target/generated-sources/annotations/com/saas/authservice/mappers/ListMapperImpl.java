package com.saas.authservice.mappers;

import com.saas.authservice.dto.request.ListTypeRequestDTO;
import com.saas.authservice.dto.response.ListTypeResponseDTO;
import com.saas.authservice.entities.ListRole;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-13T22:42:32-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class ListMapperImpl implements ListMapper {

    @Override
    public ListTypeResponseDTO toResponseDTO(ListRole entity) {
        if ( entity == null ) {
            return null;
        }

        ListTypeResponseDTO listTypeResponseDTO = new ListTypeResponseDTO();

        listTypeResponseDTO.setId( entity.getId() );
        listTypeResponseDTO.setCode( entity.getCode() );
        listTypeResponseDTO.setName( entity.getName() );
        listTypeResponseDTO.setOrder( entity.getOrder() );
        listTypeResponseDTO.setIndicatorEnabled( entity.getIndicatorEnabled() );
        listTypeResponseDTO.setAuditDate( entity.getAuditDate() );

        return listTypeResponseDTO;
    }

    @Override
    public ListRole toRoleTypeEntity(ListTypeRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ListRole listRole = new ListRole();

        listRole.setCode( dto.getCode() );
        listRole.setName( dto.getName() );
        listRole.setOrder( dto.getOrder() );
        listRole.setIndicatorEnabled( dto.getIndicatorEnabled() );

        return listRole;
    }

    @Override
    public void updateRoleTypeFromDto(ListTypeRequestDTO dto, ListRole entity) {
        if ( dto == null ) {
            return;
        }

        entity.setCode( dto.getCode() );
        entity.setName( dto.getName() );
        entity.setOrder( dto.getOrder() );
        entity.setIndicatorEnabled( dto.getIndicatorEnabled() );
    }
}
