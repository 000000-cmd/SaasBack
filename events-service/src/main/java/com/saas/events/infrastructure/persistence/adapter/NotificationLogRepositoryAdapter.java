package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.dto.PagedResponse;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.NotificationLog;
import com.saas.events.domain.model.SendStatus;
import com.saas.events.domain.port.out.INotificationLogRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.NotificationLogEntity;
import com.saas.events.infrastructure.persistence.mapper.NotificationLogPersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class NotificationLogRepositoryAdapter
        extends BaseJpaRepositoryAdapter<NotificationLog, NotificationLogEntity, UUID>
        implements INotificationLogRepositoryPort {

    private final JpaNotificationLogRepository jpa;

    public NotificationLogRepositoryAdapter(JpaNotificationLogRepository jpa,
                                             NotificationLogPersistenceMapper mapper) {
        super(jpa, mapper, "NotificationLog");
        this.jpa = jpa;
    }

    @Override
    public NotificationLog saveNow(NotificationLog row) {
        return getMapper().toDomain(jpa.saveAndFlush(getMapper().toEntity(row)));
    }

    @Override
    public Map<SendStatus, Long> countByStatus() {
        Map<SendStatus, Long> out = new EnumMap<>(SendStatus.class);
        for (Object[] row : jpa.countByStatus()) {
            out.put((SendStatus) row[0], (Long) row[1]);
        }
        return out;
    }

    @Override
    public long countSince(LocalDateTime from) {
        return jpa.countByCreatedDateGreaterThanEqual(from);
    }

    @Override
    public List<DailyCount> dailySince(LocalDateTime from) {
        List<DailyCount> out = new ArrayList<>();
        for (Object[] row : jpa.dailySince(from)) {
            LocalDate date = row[0] instanceof LocalDate ld ? ld : ((Date) row[0]).toLocalDate();
            out.add(new DailyCount(date, ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return out;
    }

    @Override
    public List<NotificationLog> lastFailures() {
        return getMapper().toDomainList(jpa.findTop5ByStatusOrderByCreatedDateDesc(SendStatus.FAILED));
    }

    @Override
    public PagedResponse<NotificationLog> search(SendStatus status, String recipient, int page, int size) {
        Specification<NotificationLogEntity> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null)                       ps.add(cb.equal(root.get("status"), status));
            if (recipient != null && !recipient.isBlank())
                ps.add(cb.like(cb.lower(root.get("recipient")), "%" + recipient.trim().toLowerCase() + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<NotificationLogEntity> result = jpa.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));

        List<NotificationLog> content = getMapper().toDomainList(result.getContent());
        return PagedResponse.of(content, page, size, result.getTotalElements());
    }

    @Override
    public long deleteOlderThan(LocalDateTime cutoff) {
        return jpa.deleteByCreatedDateBefore(cutoff);
    }
}
