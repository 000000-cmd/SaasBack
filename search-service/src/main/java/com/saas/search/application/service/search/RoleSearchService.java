package com.saas.search.application.service.search;

import com.saas.search.domain.document.RoleDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busqueda especifica de roles.
 *
 * <p>Boost: {@code code} (3x) > {@code name} (2x) > {@code description} (1x).
 */
@Service
public class RoleSearchService extends BaseSearchService<RoleDocument> {

    private final IndexNames indexNames;

    public RoleSearchService(ElasticsearchOperations ops, IndexNames indexNames) {
        super(ops);
        this.indexNames = indexNames;
    }

    @Override
    protected Class<RoleDocument> documentClass() {
        return RoleDocument.class;
    }

    @Override
    protected String aliasName() {
        return indexNames.roles();
    }

    @Override
    protected List<String> searchableFields() {
        return List.of("code^3", "name^2", "description");
    }
}
