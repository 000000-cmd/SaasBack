package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para User.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements IUserRepositoryPort {

    private final JpaUserRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public User update(User user) {
        return save(user);
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(UserEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .filter(UserEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .filter(UserEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return jpaRepository.findByUsernameOrEmail(usernameOrEmail)
                .map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return mapper.toDomainList(jpaRepository.findByVisibleTrue());
    }

    @Override
    public List<User> findAllIncludingHidden() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) return false;
        return jpaRepository.existsById(UUID.fromString(id));
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            jpaRepository.findById(UUID.fromString(id)).ifPresent(entity -> {
                entity.setVisible(false);
                entity.setEnabled(false);
                jpaRepository.save(entity);
            });
        }
    }

    @Override
    public void hardDeleteById(String id) {
        if (id != null) {
            jpaRepository.deleteById(UUID.fromString(id));
        }
    }

    @Override
    public long count() {
        return jpaRepository.findByVisibleTrue().size();
    }
}