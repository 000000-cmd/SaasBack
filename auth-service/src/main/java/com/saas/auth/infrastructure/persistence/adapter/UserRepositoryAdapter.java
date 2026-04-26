package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserRepository;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements IUserRepositoryPort {

    private final JpaUserRepository jpa;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return mapper.toDomain(jpa.save(entity));
    }

    /**
     * Update con merge: carga la entidad actual, aplica solo los campos cambiados
     * (sin tocar Id ni audit) y guarda. Asi {@code CreatedDate} y demas se preservan.
     */
    @Override
    @Transactional
    public User update(User user) {
        UserEntity existing = jpa.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "Id", user.getId()));
        mapper.updateEntityFromDomain(user, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<User> findAll() {
        return mapper.toDomainList(jpa.findAll());
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override
    @Transactional
    public void hardDeleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String value) {
        return jpa.findByUsernameOrEmail(value).map(mapper::toDomain);
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
