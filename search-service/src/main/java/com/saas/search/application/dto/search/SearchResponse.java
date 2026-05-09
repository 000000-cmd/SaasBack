package com.saas.search.application.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Respuesta paginada de busqueda.
 *
 * <p>Calcula automaticamente {@code totalPages} y {@code hasNext} a partir
 * de {@code totalHits}, {@code page} y {@code size}.
 *
 * @param <T> tipo de documento (UserDocument, RoleDocument, etc.)
 */
@Data
@Builder
public class SearchResponse<T> {

    /** Documentos del page actual. */
    private List<T> items;

    /** Total de docs que matchearon (no solo los del page actual). */
    private long totalHits;

    /** Indice de pagina actual (0-based). */
    private int page;

    /** Tamanio del page solicitado. */
    private int size;

    /** Cantidad total de paginas: {@code ceil(totalHits / size)}. */
    private int totalPages;

    /** {@code true} si existe una pagina siguiente. */
    private boolean hasNext;

    /**
     * Factory helper que calcula derivados (totalPages, hasNext).
     */
    public static <T> SearchResponse<T> of(List<T> items, long totalHits, int page, int size) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalHits / size);
        return SearchResponse.<T>builder()
                .items(items)
                .totalHits(totalHits)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .hasNext((page + 1) < totalPages)
                .build();
    }
}
