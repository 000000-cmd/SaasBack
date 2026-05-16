package com.saas.search.infrastructure.kafka.handlers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.search.domain.document.LocationDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import com.saas.search.infrastructure.kafka.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Procesa eventos de division politica y los aplica al indice {@code locations}.
 *
 * <p>Estrategia:</p>
 * <ul>
 *   <li>created: upsert documento del nivel.</li>
 *   <li>updated: upsert + (si es padre) cascade update_by_query a todos los hijos.</li>
 *   <li>deleted: delete documento + (si es padre) delete_by_query hijos.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationEventHandler implements EventHandler {

    private static final String SEP = ", ";

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.LOCATION_COUNTRY_CREATED,
            EventTypes.LOCATION_COUNTRY_UPDATED,
            EventTypes.LOCATION_COUNTRY_DELETED,
            EventTypes.LOCATION_DEPARTMENT_CREATED,
            EventTypes.LOCATION_DEPARTMENT_UPDATED,
            EventTypes.LOCATION_DEPARTMENT_DELETED,
            EventTypes.LOCATION_MUNICIPALITY_CREATED,
            EventTypes.LOCATION_MUNICIPALITY_UPDATED,
            EventTypes.LOCATION_MUNICIPALITY_DELETED,
            EventTypes.LOCATION_NEIGHBORHOOD_CREATED,
            EventTypes.LOCATION_NEIGHBORHOOD_UPDATED,
            EventTypes.LOCATION_NEIGHBORHOOD_DELETED
    );

    private final ElasticsearchOperations ops;
    private final ElasticsearchClient esClient;
    private final ObjectMapper mapper;
    private final IndexNames indexNames;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(EventEnvelope envelope) {
        try {
            String type = envelope.getType();
            JsonNode payload = envelope.getPayload();
            IndexCoordinates index = IndexCoordinates.of(indexNames.locations());

            switch (type) {
                case EventTypes.LOCATION_COUNTRY_CREATED   -> upsertCountry(payload, envelope, index, true);
                case EventTypes.LOCATION_COUNTRY_UPDATED   -> { upsertCountry(payload, envelope, index, false); cascadeCountry(payload); }
                case EventTypes.LOCATION_COUNTRY_DELETED   -> deleteCountry(payload, envelope, index);

                case EventTypes.LOCATION_DEPARTMENT_CREATED -> upsertDepartment(payload, envelope, index, true);
                case EventTypes.LOCATION_DEPARTMENT_UPDATED -> { upsertDepartment(payload, envelope, index, false); cascadeDepartment(payload); }
                case EventTypes.LOCATION_DEPARTMENT_DELETED -> deleteDepartment(payload, envelope, index);

                case EventTypes.LOCATION_MUNICIPALITY_CREATED -> upsertMunicipality(payload, envelope, index, true);
                case EventTypes.LOCATION_MUNICIPALITY_UPDATED -> { upsertMunicipality(payload, envelope, index, false); cascadeMunicipality(payload); }
                case EventTypes.LOCATION_MUNICIPALITY_DELETED -> deleteMunicipality(payload, envelope, index);

                case EventTypes.LOCATION_NEIGHBORHOOD_CREATED -> upsertNeighborhood(payload, envelope, index, true);
                case EventTypes.LOCATION_NEIGHBORHOOD_UPDATED -> upsertNeighborhood(payload, envelope, index, false);
                case EventTypes.LOCATION_NEIGHBORHOOD_DELETED -> deleteById(envelope.getAggregateId().toString(), index);

                default -> log.warn("LocationEventHandler: tipo no soportado {}", type);
            }
        } catch (Exception ex) {
            log.error("LocationEventHandler error type={} id={}: {}",
                    envelope.getType(), envelope.getAggregateId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    // ==================== UPSERTS ====================

    private void upsertCountry(JsonNode p, EventEnvelope env, IndexCoordinates index, boolean isCreate) {
        LocationDocument doc = new LocationDocument();
        doc.setId(env.getAggregateId().toString());
        doc.setBusinessId(env.getBusinessId());
        doc.setUpdatedAt(env.getOccurredAt());
        if (isCreate) doc.setCreatedAt(env.getOccurredAt());
        doc.setDocVersion(env.getOccurredAt().toEpochMilli());

        doc.setLevel("PAIS");
        doc.setCountryId(text(p, "id"));
        doc.setCountryCode(text(p, "code"));
        doc.setCountryName(text(p, "name"));
        doc.setEnabled(bool(p, "enabled"));
        String name = text(p, "name");
        doc.setSearchText(name);
        doc.setFullPath(name);

        ops.save(doc, index);
    }

    private void upsertDepartment(JsonNode p, EventEnvelope env, IndexCoordinates index, boolean isCreate) {
        LocationDocument doc = new LocationDocument();
        doc.setId(env.getAggregateId().toString());
        doc.setBusinessId(env.getBusinessId());
        doc.setUpdatedAt(env.getOccurredAt());
        if (isCreate) doc.setCreatedAt(env.getOccurredAt());
        doc.setDocVersion(env.getOccurredAt().toEpochMilli());

        doc.setLevel("DEPARTAMENTO");
        doc.setCountryId(text(p, "countryId"));
        doc.setCountryCode(text(p, "countryCode"));
        doc.setCountryName(text(p, "countryName"));
        doc.setDepartmentId(text(p, "id"));
        doc.setDepartmentCode(text(p, "code"));
        doc.setDepartmentName(text(p, "name"));
        doc.setEnabled(bool(p, "enabled"));

        doc.setSearchText(join(text(p, "name"), text(p, "countryName")));
        doc.setFullPath(join(text(p, "countryName"), text(p, "name")));

        ops.save(doc, index);
    }

    private void upsertMunicipality(JsonNode p, EventEnvelope env, IndexCoordinates index, boolean isCreate) {
        LocationDocument doc = new LocationDocument();
        doc.setId(env.getAggregateId().toString());
        doc.setBusinessId(env.getBusinessId());
        doc.setUpdatedAt(env.getOccurredAt());
        if (isCreate) doc.setCreatedAt(env.getOccurredAt());
        doc.setDocVersion(env.getOccurredAt().toEpochMilli());

        doc.setLevel("MUNICIPIO");
        doc.setCountryId(text(p, "countryId"));
        doc.setCountryCode(text(p, "countryCode"));
        doc.setCountryName(text(p, "countryName"));
        doc.setDepartmentId(text(p, "departmentId"));
        doc.setDepartmentCode(text(p, "departmentCode"));
        doc.setDepartmentName(text(p, "departmentName"));
        doc.setMunicipalityId(text(p, "id"));
        doc.setMunicipalityCode(text(p, "code"));
        doc.setMunicipalityName(text(p, "name"));
        doc.setEnabled(bool(p, "enabled"));

        doc.setSearchText(join(text(p, "name"), text(p, "departmentName"), text(p, "countryName")));
        doc.setFullPath(join(text(p, "countryName"), text(p, "departmentName"), text(p, "name")));

        ops.save(doc, index);
    }

    private void upsertNeighborhood(JsonNode p, EventEnvelope env, IndexCoordinates index, boolean isCreate) {
        LocationDocument doc = new LocationDocument();
        doc.setId(env.getAggregateId().toString());
        doc.setBusinessId(env.getBusinessId());
        doc.setUpdatedAt(env.getOccurredAt());
        if (isCreate) doc.setCreatedAt(env.getOccurredAt());
        doc.setDocVersion(env.getOccurredAt().toEpochMilli());

        String levelType = text(p, "type");
        doc.setLevel(levelType != null ? levelType : "BARRIO");

        doc.setCountryId(text(p, "countryId"));
        doc.setCountryCode(text(p, "countryCode"));
        doc.setCountryName(text(p, "countryName"));
        doc.setDepartmentId(text(p, "departmentId"));
        doc.setDepartmentCode(text(p, "departmentCode"));
        doc.setDepartmentName(text(p, "departmentName"));
        doc.setMunicipalityId(text(p, "municipalityId"));
        doc.setMunicipalityCode(text(p, "municipalityCode"));
        doc.setMunicipalityName(text(p, "municipalityName"));
        doc.setNeighborhoodId(text(p, "id"));
        doc.setNeighborhoodCode(text(p, "code"));
        doc.setNeighborhoodName(text(p, "name"));
        doc.setNeighborhoodType(text(p, "type"));
        doc.setEnabled(bool(p, "enabled"));

        doc.setSearchText(join(text(p, "name"), text(p, "municipalityName"),
                text(p, "departmentName"), text(p, "countryName")));
        doc.setFullPath(join(text(p, "countryName"), text(p, "departmentName"),
                text(p, "municipalityName"), text(p, "name")));

        ops.save(doc, index);
    }

    // ==================== CASCADE (update parent fields in children) ====================

    private void cascadeCountry(JsonNode p) throws Exception {
        String countryId = text(p, "id");
        if (countryId == null) return;
        String newCode = text(p, "code");
        String newName = text(p, "name");
        Query q = Query.of(qb -> qb.term(t -> t.field("countryId").value(countryId)));
        String script = "ctx._source.countryCode = params.code; ctx._source.countryName = params.name; ctx._source.searchText = ctx._source.searchText; ctx._source.fullPath = ctx._source.fullPath;";
        esClient.updateByQuery(u -> u
                .index(indexNames.locations())
                .query(q)
                .conflicts(co.elastic.clients.elasticsearch._types.Conflicts.Proceed)
                .script(s -> s
                        .source(script)
                        .lang("painless")
                        .params(java.util.Map.of(
                                "code", co.elastic.clients.json.JsonData.of(newCode == null ? "" : newCode),
                                "name", co.elastic.clients.json.JsonData.of(newName == null ? "" : newName)))
                )
                .refresh(true)
        );
    }

    private void cascadeDepartment(JsonNode p) throws Exception {
        String deptId = text(p, "id");
        if (deptId == null) return;
        String newCode = text(p, "code");
        String newName = text(p, "name");
        Query q = Query.of(qb -> qb.term(t -> t.field("departmentId").value(deptId)));
        String script = "ctx._source.departmentCode = params.code; ctx._source.departmentName = params.name;";
        esClient.updateByQuery(u -> u
                .index(indexNames.locations())
                .query(q)
                .conflicts(co.elastic.clients.elasticsearch._types.Conflicts.Proceed)
                .script(s -> s
                        .source(script)
                        .lang("painless")
                        .params(java.util.Map.of(
                                "code", co.elastic.clients.json.JsonData.of(newCode == null ? "" : newCode),
                                "name", co.elastic.clients.json.JsonData.of(newName == null ? "" : newName)))
                )
                .refresh(true)
        );
    }

    private void cascadeMunicipality(JsonNode p) throws Exception {
        String muniId = text(p, "id");
        if (muniId == null) return;
        String newCode = text(p, "code");
        String newName = text(p, "name");
        Query q = Query.of(qb -> qb.term(t -> t.field("municipalityId").value(muniId)));
        String script = "ctx._source.municipalityCode = params.code; ctx._source.municipalityName = params.name;";
        esClient.updateByQuery(u -> u
                .index(indexNames.locations())
                .query(q)
                .conflicts(co.elastic.clients.elasticsearch._types.Conflicts.Proceed)
                .script(s -> s
                        .source(script)
                        .lang("painless")
                        .params(java.util.Map.of(
                                "code", co.elastic.clients.json.JsonData.of(newCode == null ? "" : newCode),
                                "name", co.elastic.clients.json.JsonData.of(newName == null ? "" : newName)))
                )
                .refresh(true)
        );
    }

    // ==================== DELETES (con cascade) ====================

    private void deleteCountry(JsonNode p, EventEnvelope env, IndexCoordinates index) throws Exception {
        deleteById(env.getAggregateId().toString(), index);
        String countryId = env.getAggregateId().toString();
        esClient.deleteByQuery(d -> d
                .index(indexNames.locations())
                .query(q -> q.term(t -> t.field("countryId").value(countryId)))
                .refresh(true)
        );
    }

    private void deleteDepartment(JsonNode p, EventEnvelope env, IndexCoordinates index) throws Exception {
        deleteById(env.getAggregateId().toString(), index);
        String deptId = env.getAggregateId().toString();
        esClient.deleteByQuery(d -> d
                .index(indexNames.locations())
                .query(q -> q.term(t -> t.field("departmentId").value(deptId)))
                .refresh(true)
        );
    }

    private void deleteMunicipality(JsonNode p, EventEnvelope env, IndexCoordinates index) throws Exception {
        deleteById(env.getAggregateId().toString(), index);
        String muniId = env.getAggregateId().toString();
        esClient.deleteByQuery(d -> d
                .index(indexNames.locations())
                .query(q -> q.term(t -> t.field("municipalityId").value(muniId)))
                .refresh(true)
        );
    }

    private void deleteById(String id, IndexCoordinates index) {
        ops.delete(id, index);
    }

    // ==================== HELPERS ====================

    private static String text(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static Boolean bool(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asBoolean();
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(SEP);
            sb.append(p);
        }
        return sb.toString();
    }
}
