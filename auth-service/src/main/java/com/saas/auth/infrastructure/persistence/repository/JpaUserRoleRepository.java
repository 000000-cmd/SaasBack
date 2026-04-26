package com.saas.auth.infrastructure.persistence.repository;

import com.saas.auth.infrastructure.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaUserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query("SELECT ur FROM UserRoleEntity ur WHERE ur.user.id = :userId")
    List<UserRoleEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT (COUNT(ur) > 0) FROM UserRoleEntity ur WHERE ur.user.id = :userId AND ur.roleId = :roleId")
    boolean existsByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Modifying
    @Query("DELETE FROM UserRoleEntity ur WHERE ur.user.id = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
