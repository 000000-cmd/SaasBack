package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter
        extends BaseJpaRepositoryAdapter<User, UserEntity, UUID>
        implements IUserRepositoryPort {

    private final JpaUserRepository jpa;

    public UserRepositoryAdapter(JpaUserRepository jpa, UserPersistenceMapper mapper) {
        super(jpa, mapper, "Usuario");
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(getMapper()::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(getMapper()::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String value) {
        return jpa.findByUsernameOrEmail(value).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
