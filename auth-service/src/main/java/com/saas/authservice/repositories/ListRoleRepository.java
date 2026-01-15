package com.saas.authservice.repositories;

import com.saas.authservice.entities.ListRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListRoleRepository extends JpaRepository<ListRole, UUID> {
    Optional<ListRole> findByCode(String code);
}
