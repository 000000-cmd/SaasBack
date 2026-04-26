package com.saas.auth.infrastructure.persistence.repository;

import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (LOWER(u.username) = LOWER(:value) OR LOWER(u.email) = LOWER(:value))
            """)
    Optional<UserEntity> findByUsernameOrEmail(@Param("value") String value);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
