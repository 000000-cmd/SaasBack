package com.saas.search.application.service.search;

import com.saas.search.domain.document.UserDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busqueda especifica de usuarios.
 *
 * <p>Boost de relevancia:
 * <ul>
 *   <li>{@code username^3}: match en username = MUY relevante (3x).</li>
 *   <li>{@code email^2}: match en email = relevante (2x).</li>
 *   <li>{@code fullName}: match en nombre completo = base (1x).</li>
 * </ul>
 */
@Service
public class UserSearchService extends BaseSearchService<UserDocument> {

    private final IndexNames indexNames;

    public UserSearchService(ElasticsearchOperations ops, IndexNames indexNames) {
        super(ops);
        this.indexNames = indexNames;
    }

    @Override
    protected Class<UserDocument> documentClass() {
        return UserDocument.class;
    }

    @Override
    protected String aliasName() {
        return indexNames.users();
    }

    @Override
    protected List<String> searchableFields() {
        return List.of("username^3", "email^2", "fullName");
    }
}
