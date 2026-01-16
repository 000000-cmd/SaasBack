package com.saas.systemservice.infrastructure.adapters.out.persistence.repository.menu;

import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaMenuRepository extends JpaRepository<MenuEntity, UUID> {
    Optional<MenuEntity> findByCode(String code);
    boolean existsByCode(String code);
}
