package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.DynamicList;
import com.saas.system.domain.port.out.IDynamicListRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de repositorio para items de listas dinámicas.
 * Usa JDBC para ejecutar consultas dinámicas sobre diferentes tablas.
 *
 * Todas las tablas de listas deben tener la siguiente estructura:
 * - id (UUID, PK)
 * - code (VARCHAR, UNIQUE)
 * - name (VARCHAR)
 * - display_order (INT)
 * - enabled (BOOLEAN)
 * - visible (BOOLEAN)
 * - audit_user (VARCHAR)
 * - audit_date (DATETIME)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamicListRepositoryAdapter implements IDynamicListRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    // RowMapper para convertir ResultSet a DynamicList
    private final RowMapper<DynamicList> rowMapper = (rs, rowNum) -> mapRow(rs);

    private DynamicList mapRow(ResultSet rs) throws SQLException {
        DynamicList item = new DynamicList();
        item.setId(rs.getString("id"));
        item.setCode(rs.getString("code"));
        item.setName(rs.getString("name"));
        item.setDisplayOrder(rs.getInt("display_order"));
        item.setEnabled(rs.getBoolean("enabled"));
        item.setVisible(rs.getBoolean("visible"));
        item.setAuditUser(rs.getString("audit_user"));
        item.setAuditDate(rs.getTimestamp("audit_date") != null
                ? rs.getTimestamp("audit_date").toLocalDateTime()
                : null);
        return item;
    }

    @Override
    public DynamicList save(String tableName, DynamicList item) {
        validateTableName(tableName);

        String id = item.getId() != null ? item.getId() : UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        String sql = String.format("""
            INSERT INTO %s (id, code, name, display_order, enabled, visible, audit_user, audit_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, tableName);

        jdbcTemplate.update(sql,
                id,
                item.getCode(),
                item.getName(),
                item.getDisplayOrder(),
                item.getEnabled() != null ? item.getEnabled() : true,
                item.getVisible() != null ? item.getVisible() : true,
                item.getAuditUser() != null ? item.getAuditUser() : "SYSTEM",
                now);

        item.setId(id);
        item.setAuditDate(now);
        item.setEnabled(item.getEnabled() != null ? item.getEnabled() : true);
        item.setVisible(item.getVisible() != null ? item.getVisible() : true);

        log.debug("Item guardado en {}: {} (ID: {})", tableName, item.getCode(), id);
        return item;
    }

    @Override
    public DynamicList update(String tableName, DynamicList item) {
        validateTableName(tableName);

        LocalDateTime now = LocalDateTime.now();

        String sql = String.format("""
            UPDATE %s 
            SET code = ?, name = ?, display_order = ?, audit_user = ?, audit_date = ?
            WHERE id = ?
            """, tableName);

        int updated = jdbcTemplate.update(sql,
                item.getCode(),
                item.getName(),
                item.getDisplayOrder(),
                item.getAuditUser() != null ? item.getAuditUser() : "SYSTEM",
                now,
                item.getId());

        if (updated > 0) {
            item.setAuditDate(now);
            log.debug("Item actualizado en {}: {} (ID: {})", tableName, item.getCode(), item.getId());
        }

        return item;
    }

    @Override
    public List<DynamicList> findAll(String tableName) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT * FROM %s ORDER BY display_order, name
            """, tableName);

        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<DynamicList> findAllVisible(String tableName) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT * FROM %s WHERE visible = true ORDER BY display_order, name
            """, tableName);

        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<DynamicList> findAllEnabled(String tableName) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT * FROM %s WHERE enabled = true AND visible = true ORDER BY display_order, name
            """, tableName);

        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Optional<DynamicList> findById(String tableName, String id) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT * FROM %s WHERE id = ? AND visible = true
            """, tableName);

        List<DynamicList> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<DynamicList> findByCode(String tableName, String code) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT * FROM %s WHERE code = ? AND visible = true
            """, tableName);

        List<DynamicList> results = jdbcTemplate.query(sql, rowMapper, code);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean existsByCode(String tableName, String code) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT COUNT(*) FROM %s WHERE code = ? AND visible = true
            """, tableName);

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code);
        return count != null && count > 0;
    }

    @Override
    public boolean existsById(String tableName, String id) {
        validateTableName(tableName);

        String sql = String.format("""
            SELECT COUNT(*) FROM %s WHERE id = ? AND visible = true
            """, tableName);

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public void softDelete(String tableName, String id) {
        validateTableName(tableName);

        String sql = String.format("""
            UPDATE %s SET visible = false, enabled = false, audit_date = ? WHERE id = ?
            """, tableName);

        jdbcTemplate.update(sql, LocalDateTime.now(), id);
        log.debug("Item eliminado (soft) en {}: ID {}", tableName, id);
    }

    @Override
    public void hardDelete(String tableName, String id) {
        validateTableName(tableName);

        String sql = String.format("""
            DELETE FROM %s WHERE id = ?
            """, tableName);

        jdbcTemplate.update(sql, id);
        log.debug("Item eliminado (hard) en {}: ID {}", tableName, id);
    }

    @Override
    public void toggleEnabled(String tableName, String id, boolean enabled) {
        validateTableName(tableName);

        String sql = String.format("""
            UPDATE %s SET enabled = ?, audit_date = ? WHERE id = ?
            """, tableName);

        jdbcTemplate.update(sql, enabled, LocalDateTime.now(), id);
        log.debug("Estado de item en {} cambiado a {}: ID {}", tableName, enabled, id);
    }

    @Override
    public boolean tableExists(String tableName) {
        validateTableName(tableName);

        try {
            String sql = String.format("SELECT 1 FROM %s LIMIT 1", tableName);
            jdbcTemplate.queryForObject(sql, Integer.class);
            return true;
        } catch (Exception e) {
            log.debug("Tabla {} no existe: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Valida que el nombre de la tabla sea seguro para evitar SQL injection.
     * Solo permite nombres que empiecen con 'sys_list_' y contengan caracteres alfanuméricos y guiones bajos.
     */
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tabla no puede ser nulo o vacío");
        }

        // Solo permitir nombres que empiecen con sys_list_ y contengan caracteres seguros
        if (!tableName.matches("^sys_list_[a-z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Nombre de tabla inválido. Debe seguir el patrón: sys_list_nombre_en_minusculas");
        }
    }
}