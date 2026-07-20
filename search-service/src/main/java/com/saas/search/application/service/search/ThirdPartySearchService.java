package com.saas.search.application.service.search;

import com.saas.search.domain.document.ThirdPartyDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Tarjetas de persona (nombre + foto) en lote, leidas del read model de ES.
     * Sustituye a la lectura directa contra thirdparty-service en los listados.
     */
    public Map<String, PersonCard> cardsByIds(List<String> ids) {
        Map<String, PersonCard> out = new HashMap<>();
        IndexCoordinates index = IndexCoordinates.of(indexNames.thirdParties());
        for (String id : ids) {
            if (id == null) continue;
            ThirdPartyDocument doc = ops.get(id, ThirdPartyDocument.class, index);
            if (doc != null) out.put(id, new PersonCard(doc.getFullName(), doc.getPhotoUrl()));
        }
        return out;
    }

    /**
     * Tercero vinculado a una cuenta. Evita el viaje a la BD transaccional en
     * lecturas de visualizacion (perfil, "mi negocio").
     */
    public ThirdPartyDocument findByUserId(String userId) {
        return findOne(Criteria.where("userId").is(userId));
    }

    /**
     * Cuenta duena de un numero de documento (login flexible). Es la ruta mas
     * caliente de todas: se resuelve contra el read model, no contra la BD.
     */
    public ThirdPartyDocument findByDocumentNumber(String documentNumber) {
        return findOne(Criteria.where("documentNumber").is(documentNumber));
    }

    private ThirdPartyDocument findOne(Criteria criteria) {
        SearchHits<ThirdPartyDocument> hits = ops.search(
                new CriteriaQuery(criteria), ThirdPartyDocument.class,
                IndexCoordinates.of(indexNames.thirdParties()));
        return hits.getTotalHits() > 0 ? hits.getSearchHit(0).getContent() : null;
    }

    public record PersonCard(String fullName, String photoUrl) {}
}
