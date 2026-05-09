package com.saas.common.persistence;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.mapper.IBaseMapper;
import com.saas.common.model.BaseDomain;
import com.saas.common.port.out.IGenericRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter base para repositorios JPA. Implementa el CRUD generico
 * ({@link IGenericRepositoryPort}) delegando en un {@link JpaRepository} y un
 * {@link IBaseMapper} concretos.
 *
 * <p>Cada adapter especifico:
 * <ul>
 *   <li>Extiende esta clase con sus tipos concretos.</li>
 *   <li>Pasa al constructor su {@code JpaRepository}, su mapper y el nombre
 *       del recurso (usado en mensajes de {@link ResourceNotFoundException}).</li>
 *   <li>Solo escribe los metodos especificos del dominio (busquedas custom,
 *       queries adhoc, etc.). El CRUD universal lo hereda.</li>
 * </ul>
 *
 * <h3>Ejemplo:</h3>
 * <pre>{@code
 * @Repository
 * public class UserRepositoryAdapter
 *         extends BaseJpaRepositoryAdapter<User, UserEntity, UUID>
 *         implements IUserRepositoryPort {
 *
 *     private final JpaUserRepository jpa;
 *
 *     public UserRepositoryAdapter(JpaUserRepository jpa, UserPersistenceMapper mapper) {
 *         super(jpa, mapper, "Usuario");
 *         this.jpa = jpa;
 *     }
 *
 *     @Override
 *     public Optional<User> findByUsername(String username) {
 *         return jpa.findByUsername(username).map(getMapper()::toDomain);
 *     }
 *     // ... resto de metodos especificos
 * }
 * }</pre>
 *
 * @param <D>  dominio (debe extender {@link BaseDomain})
 * @param <E>  entidad JPA (debe extender {@link BaseEntity})
 * @param <ID> tipo del identificador (tipicamente UUID)
 */
public abstract class BaseJpaRepositoryAdapter<D extends BaseDomain, E extends BaseEntity, ID>
        implements IGenericRepositoryPort<D, ID> {

    protected final JpaRepository<E, ID> jpa;
    protected final IBaseMapper<D, E> mapper;
    private final String resourceName;

    protected BaseJpaRepositoryAdapter(JpaRepository<E, ID> jpa,
                                       IBaseMapper<D, E> mapper,
                                       String resourceName) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.resourceName = resourceName;
    }

    /** Acceso al mapper para subclases que lo necesiten en metodos especificos. */
    protected IBaseMapper<D, E> getMapper() {
        return mapper;
    }

    @Override
    public D save(D domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    /**
     * Update con merge: carga la entidad actual, aplica solo los campos cambiados
     * (sin tocar Id ni audit) y guarda. Asi {@code CreatedDate} y demas se preservan.
     */
    @Override
    @Transactional
    public D update(D domain) {
        @SuppressWarnings("unchecked")
        ID id = (ID) domain.getId();
        E existing = jpa.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, "Id", id));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override
    public Optional<D> findById(ID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(ID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<D> findAll() {
        return mapper.toDomainList(jpa.findAll());
    }

    @Override
    public List<D> findAllPaged(int page, int size) {
        return mapper.toDomainList(jpa.findAll(PageRequest.of(page, size)).getContent());
    }

    @Override
    public long count() {
        return jpa.count();
    }

    /**
     * Soft-delete: marca {@code Enabled = false} y {@code Visible = false}.
     * Si la entidad no existe, no hace nada (operacion idempotente).
     */
    @Override
    @Transactional
    public void softDeleteById(ID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override
    @Transactional
    public void hardDeleteById(ID id) {
        jpa.deleteById(id);
    }
}
