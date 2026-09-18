package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessPublicProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * El directorio publico.
 *
 * <h3>De donde sale cada cosa</h3>
 * <p>De la ficha ({@code business_public_profile}) solo salen tres cosas: si
 * quiere aparecer, si esta verificado y como lo califican. TODO lo demas se lee
 * de donde el dueño ya lo escribio:</p>
 * <ul>
 *   <li>nombre, logo y categoria → {@code business} (+ {@code business_type}),
 *       elegidos al crear el negocio;</li>
 *   <li>titular y portada → {@code business_landing}, de «Mi pagina»;</li>
 *   <li>direccion, ciudad y mapa → {@code branch}, de «Sedes».</li>
 * </ul>
 * <p>Antes estaban copiados aqui, y una copia no se mantiene sola: el dueño
 * cambiaba la direccion en Sedes y el directorio seguia enseñando la vieja sin
 * que nada fallara.</p>
 *
 * <p>La pagina se lee AUNQUE no este publicada: publicar la pagina y aparecer
 * en el directorio son dos decisiones distintas, y la que manda aqui es
 * {@code IsListed}. Si no, encender el directorio no enseñaria nada.</p>
 *
 * <h3>Por que consulta nativa</h3>
 * <p>Una ficha de directorio cruza seis tablas —negocio, tipo, ficha, dominio,
 * sede y municipio— y se ordena por una formula. En JPQL saldrian seis
 * entidades cargadas enteras para leer diez columnas.</p>
 *
 * <h3>El orden es bayesiano, y eso NO es un adorno</h3>
 * <p>Ordenar por media pura pone primero al que tiene un unico cinco y hunde al
 * que tiene cuarenta resenas con 4,8. La formula tira de la media hacia la
 * media global mientras haya pocas opiniones:</p>
 * <pre>
 *   score = (v/(v+m))·R + (m/(v+m))·C
 * </pre>
 * <p>donde {@code R} es la media del negocio, {@code v} sus resenas, {@code C}
 * la media global y {@code m} cuantas hacen falta para creerse la propia. Con
 * cero resenas el score ES la media global: un negocio nuevo no sale ni el
 * primero ni el ultimo, sale en el medio, que es lo honesto.</p>
 */
public interface JpaMarketplaceRepository extends JpaRepository<BusinessPublicProfileEntity, UUID> {

    /** Cuantas resenas hacen falta para que la media propia pese de verdad. */
    int PESO_MINIMO = 5;

    /** Una fila del directorio, ya resuelta. */
    interface Card {
        String getBusinessId();
        String getSlug();
        String getName();
        String getHeadline();
        String getCategoryCode();
        String getCoverImageUrl();
        String getLogoUrl();
        String getCity();
        Integer getReviewCount();
        Integer getStarSum();
        Double getScore();
        Boolean getIsVerified();
        Integer getServiceCount();
        java.math.BigDecimal getMinPrice();
        /** De la sede. NULL = no sale en el mapa, pero si en el listado. */
        java.math.BigDecimal getLatitude();
        java.math.BigDecimal getLongitude();
        String getAddressLine();
    }

    /**
     * La sede que representa al negocio en el directorio: la principal, y si
     * ninguna esta marcada como tal, la primera que se dio de alta — antes que
     * no enseñar direccion ninguna.
     *
     * <p>Un pin por negocio: uno con tres sedes sale una vez. Enseñar las tres
     * es otra consulta y otra tarjeta, y hoy nadie lo ha pedido.</p>
     */
    String SEDE = """
        SELECT br.Id FROM branch br
         WHERE br.BusinessId = b.Id AND br.Visible = 1 AND br.Enabled = 1
         ORDER BY br.IsMain DESC, br.CreatedDate ASC LIMIT 1
        """;

    String SELECT = """
        SELECT b.Id            AS businessId,
               d.Slug          AS slug,
               COALESCE(NULLIF(b.TradeName,''), b.Name) AS name,
               NULLIF(l.Tagline,'')      AS headline,
               t.Code                    AS categoryCode,
               NULLIF(l.HeroImageUrl,'') AS coverImageUrl,
               b.LogoUrl       AS logoUrl,
               m.Name          AS city,
               p.ReviewCount   AS reviewCount,
               p.StarSum       AS starSum,
               p.IsVerified    AS isVerified,
               (SELECT COUNT(*) FROM business_offering o
                 WHERE o.BusinessId = b.Id AND o.IsActive = 1 AND o.Visible = 1) AS serviceCount,
               (SELECT MIN(o.Price) FROM business_offering o
                 WHERE o.BusinessId = b.Id AND o.IsActive = 1 AND o.Visible = 1) AS minPrice,
               s.Latitude    AS latitude,
               s.Longitude   AS longitude,
               s.AddressLine AS addressLine,
               ((p.ReviewCount / (p.ReviewCount + :peso)) * (p.StarSum / GREATEST(p.ReviewCount, 1))
                 + (:peso / (p.ReviewCount + :peso)) * :mediaGlobal) AS score
        FROM business_public_profile p
        JOIN business b        ON b.Id = p.BusinessId AND b.Visible = 1 AND b.Enabled = 1
        JOIN business_type t   ON t.Id = b.BusinessTypeId
        JOIN business_domain d ON d.BusinessId = b.Id AND d.IsPrimary = 1 AND d.Visible = 1
        LEFT JOIN business_landing l ON l.BusinessId = b.Id AND l.Visible = 1
        LEFT JOIN branch s ON s.Id = (
        """ + SEDE + """
        )
        LEFT JOIN municipality m ON m.Id = s.MunicipalityId
        WHERE p.Visible = 1 AND p.IsListed = 1
          AND (:texto IS NULL
               OR COALESCE(NULLIF(b.TradeName,''), b.Name) LIKE CONCAT('%', :texto, '%')
               OR l.Tagline LIKE CONCAT('%', :texto, '%')
               OR EXISTS (SELECT 1 FROM business_offering o
                           WHERE o.BusinessId = b.Id AND o.Visible = 1 AND o.IsActive = 1
                             AND o.Name LIKE CONCAT('%', :texto, '%')))
          AND (:categoria IS NULL OR t.Code = :categoria)
          AND (:municipio IS NULL OR s.MunicipalityId = :municipio)
        """;

    @Query(value = SELECT + " ORDER BY score DESC, p.ReviewCount DESC, name ASC "
                 + " LIMIT :limite OFFSET :desde", nativeQuery = true)
    List<Card> directory(@Param("texto") String texto,
                         @Param("categoria") String categoria,
                         @Param("municipio") String municipio,
                         @Param("mediaGlobal") double mediaGlobal,
                         @Param("peso") int peso,
                         @Param("limite") int limite,
                         @Param("desde") int desde);

    @Query(value = "SELECT COUNT(*) FROM (" + SELECT + ") fila", nativeQuery = true)
    long countDirectory(@Param("texto") String texto,
                        @Param("categoria") String categoria,
                        @Param("municipio") String municipio,
                        @Param("mediaGlobal") double mediaGlobal,
                        @Param("peso") int peso);

    /** La ficha de un negocio, con los mismos origenes que el listado. */
    interface Detail {
        String getBusinessId();
        String getSlug();
        String getName();
        String getHeadline();
        String getDescription();
        String getCategoryCode();
        String getCoverImageUrl();
        String getLogoUrl();
        String getAddressLine();
        java.math.BigDecimal getLatitude();
        java.math.BigDecimal getLongitude();
        Integer getReviewCount();
        Integer getStarSum();
        Boolean getIsVerified();
    }

    /**
     * La ficha por subdominio.
     *
     * <p>Va por la MISMA consulta que el listado, no por las entidades: si la
     * tarjeta y la ficha leyeran de sitios distintos acabarian diciendo cosas
     * distintas del mismo negocio.</p>
     */
    @Query(value = """
        SELECT b.Id            AS businessId,
               d.Slug          AS slug,
               COALESCE(NULLIF(b.TradeName,''), b.Name) AS name,
               NULLIF(l.Tagline,'')      AS headline,
               NULLIF(l.About,'')        AS description,
               t.Code                    AS categoryCode,
               NULLIF(l.HeroImageUrl,'') AS coverImageUrl,
               b.LogoUrl       AS logoUrl,
               s.AddressLine   AS addressLine,
               s.Latitude      AS latitude,
               s.Longitude     AS longitude,
               p.ReviewCount   AS reviewCount,
               p.StarSum       AS starSum,
               p.IsVerified    AS isVerified
        FROM business_public_profile p
        JOIN business b        ON b.Id = p.BusinessId AND b.Visible = 1 AND b.Enabled = 1
        JOIN business_type t   ON t.Id = b.BusinessTypeId
        JOIN business_domain d ON d.BusinessId = b.Id AND d.IsPrimary = 1 AND d.Visible = 1
        LEFT JOIN business_landing l ON l.BusinessId = b.Id AND l.Visible = 1
        LEFT JOIN branch s ON s.Id = (
        """ + SEDE + """
        )
        WHERE p.Visible = 1 AND p.IsListed = 1 AND d.Slug = :slug
        """, nativeQuery = true)
    java.util.Optional<Detail> detailBySlug(@Param("slug") String slug);

    /**
     * Lo mismo, pero para el propio dueño y ESTE aparezca o no.
     *
     * <p>Es la vista previa de su pantalla de directorio: le enseña la ficha que
     * se publicaria con lo que ya tiene escrito, para que vea que no hay nada
     * que volver a teclear y donde falta algo.</p>
     *
     * <p>Arranca de {@code business} y no de la ficha porque un negocio que
     * nunca pulso «publicar» no TIENE fila de ficha, y es justo el que mas
     * necesita ver la vista previa. Por eso los contadores pueden llegar
     * nulos.</p>
     */
    @Query(value = """
        SELECT b.Id            AS businessId,
               d.Slug          AS slug,
               COALESCE(NULLIF(b.TradeName,''), b.Name) AS name,
               NULLIF(l.Tagline,'')      AS headline,
               NULLIF(l.About,'')        AS description,
               t.Code                    AS categoryCode,
               NULLIF(l.HeroImageUrl,'') AS coverImageUrl,
               b.LogoUrl       AS logoUrl,
               s.AddressLine   AS addressLine,
               s.Latitude      AS latitude,
               s.Longitude     AS longitude,
               -- Sin COALESCE a proposito: envolverlos hace que MySQL los
               -- devuelva como BIGINT y la proyeccion no sabe meter un Long en
               -- un Integer ni en un Boolean. Llegan nulos y se resuelven en
               -- Java, que es donde ya se sabe que nulo aqui significa cero.
               p.ReviewCount   AS reviewCount,
               p.StarSum       AS starSum,
               p.IsVerified    AS isVerified
        FROM business b
        JOIN business_type t   ON t.Id = b.BusinessTypeId
        JOIN business_domain d ON d.BusinessId = b.Id AND d.IsPrimary = 1 AND d.Visible = 1
        LEFT JOIN business_landing l ON l.BusinessId = b.Id AND l.Visible = 1
        LEFT JOIN branch s ON s.Id = (
        """ + SEDE + """
        )
        LEFT JOIN business_public_profile p ON p.BusinessId = b.Id AND p.Visible = 1
        WHERE b.Id = :businessId AND b.Visible = 1
        """, nativeQuery = true)
    java.util.Optional<Detail> previewOf(@Param("businessId") String businessId);

    /**
     * La media global, que es el ancla de la formula.
     *
     * <p>Solo cuenta los negocios que YA tienen resenas: incluir a los que no
     * tienen ninguna arrastraria el ancla a cero y el orden dejaria de tener
     * sentido.</p>
     */
    @Query(value = "SELECT COALESCE(SUM(StarSum) / NULLIF(SUM(ReviewCount), 0), 4.0) "
                 + "FROM business_public_profile WHERE Visible = 1 AND IsListed = 1",
           nativeQuery = true)
    Double globalAverage();

    /**
     * Las categorias que de verdad tienen negocios listados.
     *
     * <p>Salen del tipo de negocio elegido al crearlo: no hay una segunda lista
     * de categorias que mantener al dia.</p>
     */
    @Query(value = """
        SELECT DISTINCT t.Code FROM business_public_profile p
          JOIN business b      ON b.Id = p.BusinessId AND b.Visible = 1 AND b.Enabled = 1
          JOIN business_type t ON t.Id = b.BusinessTypeId
         WHERE p.Visible = 1 AND p.IsListed = 1
         ORDER BY t.Code
        """, nativeQuery = true)
    List<String> categories();
}
