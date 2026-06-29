package com.saas.search.application.service.search;

import com.saas.search.domain.document.ThirdPartyDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busqueda especifica de Terceros.
 *
 * <p>Boost de relevancia:
 * <ul>
 *   <li>{@code fullName^3}: nombre completo = muy relevante.</li>
 *   <li>{@code documentNumber^2}: numero de documento = relevante.</li>
 *   <li>{@code businessName, tradeName, email}: base.</li>
 * </ul>
 */
@Service
public class ThirdPartySearchService extends BaseSearchService<ThirdPartyDocument> {

    private final IndexNames indexNames;

    public ThirdPartySearchService(ElasticsearchOperations ops, IndexNames indexNames) {
        super(ops);
        this.indexNames = indexNames;
    }

    @Override
    protected Class<ThirdPartyDocument> documentClass() {
        return ThirdPartyDocument.class;
    }

    @Override
    protected String aliasName() {
        return indexNames.thirdParties();
    }

    @Override
    protected List<String> searchableFields() {
        return List.of("fullName^3", "documentNumber^2", "firstName", "firstLastName");
    }

    /** Obtiene el documento del tercero por id (para comparar Elastic vs BD). */
    public ThirdPartyDocument findById(String id) {
        return ops.get(id, ThirdPartyDocument.class, IndexCoordinates.of(indexNames.thirdParties()));
    }
}
