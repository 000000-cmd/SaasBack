package com.saas.authservice.services;

import com.saas.authservice.dto.request.ListTypeRequestDTO;
import com.saas.authservice.dto.response.ListTypeResponseDTO;
import com.saas.authservice.entities.ListRole;
import com.saas.authservice.mappers.ListMapper;
import com.saas.authservice.repositories.ListRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class ListRoleService extends AbstractListService<ListRole, ListRole, ListRoleRepository> {

    @Autowired
    public ListRoleService(ListRoleRepository repository, ListMapper mapper) {
        super(repository, mapper, "Tipo de Rol");
    }

    @Override
    @Transactional(readOnly = true)
    protected Function<ListTypeRequestDTO, ListRole> toEntity() {
        return mapper::toRoleTypeEntity;
    }


    @Override
    @Transactional(readOnly = true)
    protected Function<ListRole, ListTypeResponseDTO> toResponseDTO() {
        return mapper::toResponseDTO;
    }


    @Transactional
    protected BiConsumer<ListTypeRequestDTO, ListRole> updateFromDto() {
        return mapper::updateRoleTypeFromDto;
    }

    @Override
    @Transactional
    protected void setSharedProperties(ListRole entity) {
        entity.setId(UUID.randomUUID());
        entity.setAuditDate(LocalDateTime.now());
    }

    @Override
    @Transactional // Part of the update transaction
    protected void setAuditDate(ListRole entity, LocalDateTime date) {
        entity.setAuditDate(date);
    }

    @Override
    @Transactional // Write operation
    public ListTypeResponseDTO create(ListTypeRequestDTO requestDTO) {
        repository.findByCode(requestDTO.getCode()).ifPresent(e -> {
            throw new IllegalArgumentException("El código '" + requestDTO.getCode() + "' ya existe para " + entityName);
        });
        return super.create(requestDTO);
    }

}