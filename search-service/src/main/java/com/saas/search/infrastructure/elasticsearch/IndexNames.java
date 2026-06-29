package com.saas.search.infrastructure.elasticsearch;

import org.springframework.stereotype.Component;

/**
 * Catalogo central de nombres de aliases de Elasticsearch.
 *
 * <p>Importante: estos son ALIASES, no nombres de indice fisicos. Por debajo,
 * cada alias apunta a una version del indice ({@code users_v1}, {@code users_v2}, ...).
 * Cuando hagas reindex, se cambia el alias atomicamente sin que el codigo se entere.
 *
 * <p>Convencion: un solo nombre por entidad, sin sufijos de version.
 */
@Component("indexNames")
public class IndexNames {

    public String users() { return "users"; }

    public String roles() { return "roles"; }

    public String locations() { return "locations"; }

    public String thirdParties() { return "third_parties"; }

}
