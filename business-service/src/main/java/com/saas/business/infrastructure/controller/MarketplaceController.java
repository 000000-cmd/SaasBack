package com.saas.business.infrastructure.controller;

import com.saas.business.application.service.ReviewService;
import com.saas.business.domain.model.AppointmentReview;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IAppointmentReviewRepositoryPort;
import com.saas.business.domain.port.out.IBusinessDomainRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.business.infrastructure.persistence.repository.JpaMarketplaceRepository;
import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El directorio publico de negocios.
 *
 * <p>Sin sesion: es un escaparate, y pedir cuenta para mirar es la forma mas
 * rapida de que nadie mire. Lo que se publica es SOLO lo que el negocio
 * encendio en su ficha ({@code isListed}), nunca su operacion.</p>
 *
 * <p>El detalle se resuelve por SLUG, igual que la reserva publica: el negocio
 * sale de la ruta y jamas del cuerpo.</p>
 */
@RestController
@RequestMapping("/public/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private static final int MAX_POR_PAGINA = 24;
    private static final int RESENAS_EN_FICHA = 20;

    private final JpaMarketplaceRepository directorio;
    private final IBusinessDomainRepositoryPort domains;
    private final IOfferingUseCase offerings;
    private final IAppointmentReviewRepositoryPort reviews;
    private final IEmployeeRepositoryPort employees;
    private final ThirdPartyClient people;
    private final ReviewService reviewService;

    // ------------------------------------------------------------- listado

    /**
     * Una tarjeta del directorio.
     *
     * <p>Nada de esto se guarda para el directorio: el nombre y el logo son los
     * del negocio, la categoria es su tipo, el titular y la portada son los de
     * su pagina, y la direccion la de su sede. El directorio los LEE.</p>
     *
     * @param latitude donde esta la sede. Va {@code null} mientras el negocio no
     *        la marque, y entonces sale en el listado pero NO en el mapa — que
     *        es mejor que un pin puesto en el centro de la ciudad.
     */
    public record CardView(
            String slug, String name, String headline, String categoryCode,
            String coverImageUrl, String logoUrl, String city, String address,
            Integer reviewCount, BigDecimal rating, boolean verified,
            Integer serviceCount, BigDecimal minPrice,
            BigDecimal latitude, BigDecimal longitude) {}

    /**
     * El listado, ordenado por reputacion bayesiana.
     *
     * <p>Ordenar por media pura pondria primero al que tiene un unico cinco.
     * La formula tira de la media hacia la global mientras haya pocas opiniones,
     * asi que un negocio nuevo sale en el medio y no arriba del todo.</p>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int limite = Math.min(Math.max(size, 1), MAX_POR_PAGINA);
        int desde = Math.max(page, 0) * limite;
        String texto = (q == null || q.isBlank()) ? null : q.trim();
        String muni = municipalityId == null ? null : municipalityId.toString();
        double media = directorio.globalAverage() == null ? 4.0 : directorio.globalAverage();

        List<CardView> items = directorio
                .directory(texto, blankToNull(category), muni, media,
                           JpaMarketplaceRepository.PESO_MINIMO, limite, desde)
                .stream().map(MarketplaceController::toCard).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", directorio.countDirectory(texto, blankToNull(category), muni, media,
                JpaMarketplaceRepository.PESO_MINIMO));
        out.put("page", Math.max(page, 0));
        out.put("size", limite);
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    /** Las categorias que de verdad tienen negocios listados. */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> categories() {
        return ResponseEntity.ok(ApiResponse.success(directorio.categories()));
    }

    // -------------------------------------------------------------- ficha

    public record ReviewView(Integer businessStars, Integer employeeStars, String comment,
                             String employeeName, String reply, String createdAt) {}

    public record DetailView(
            String slug, String name, String headline, String description,
            String categoryCode, String coverImageUrl, String logoUrl,
            String addressLine, BigDecimal latitude, BigDecimal longitude,
            Integer reviewCount, BigDecimal rating, boolean verified,
            List<ServiceView> services, List<ReviewView> reviews) {}

    public record ServiceView(String name, String description,
                              Integer durationMinutes, BigDecimal price) {}

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<DetailView>> detail(@PathVariable String slug) {
        // Una sola consulta, la MISMA que arma las tarjetas del listado: asi la
        // ficha y la tarjeta de un negocio no pueden decir cosas distintas.
        JpaMarketplaceRepository.Detail f = directorio.detailBySlug(slug.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "slug", slug));

        UUID businessId = UUID.fromString(f.getBusinessId());

        List<ServiceView> servicios = offerings.findByBusiness(businessId).stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .sorted((a, b) -> a.getPrice().compareTo(b.getPrice()))
                .map(o -> new ServiceView(o.getName(), o.getDescription(),
                        o.getDurationMinutes(), o.getPrice()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new DetailView(
                slug,
                f.getName(),
                f.getHeadline(),
                f.getDescription(),
                f.getCategoryCode(),
                f.getCoverImageUrl(),
                f.getLogoUrl(),
                f.getAddressLine(),
                f.getLatitude(),
                f.getLongitude(),
                f.getReviewCount(),
                media(f.getReviewCount(), f.getStarSum()),
                Boolean.TRUE.equals(f.getIsVerified()),
                servicios,
                resenas(businessId))));
    }

    // ------------------------------------------------------------ resenas

    public record ReviewRequest(
            @NotBlank String publicCode,
            @NotBlank String last4,
            @NotNull Integer businessStars,
            Integer employeeStars,
            String comment) {}

    /**
     * Dejar la opinion de una cita.
     *
     * <p>El negocio sale del SLUG de la ruta. Si viniera en el cuerpo, se
     * podria colgar una resena de la ficha de otro.</p>
     */
    @PostMapping("/{slug}/reviews")
    public ResponseEntity<ApiResponse<Void>> review(@PathVariable String slug,
                                                    @Valid @RequestBody ReviewRequest req) {
        UUID businessId = domains.findBySlug(slug.trim().toLowerCase())
                .map(d -> d.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "slug", slug));

        reviewService.crear(businessId, new ReviewService.NuevaResena(
                req.publicCode(), req.last4(), req.businessStars(),
                req.employeeStars(), req.comment()));

        return ResponseEntity.ok(ApiResponse.success(null, "Gracias por tu opinión"));
    }

    // ----------------------------------------------------------- privados

    /** Las opiniones publicadas, con el nombre de quien atendio. */
    private List<ReviewView> resenas(UUID businessId) {
        List<AppointmentReview> filas = reviews.publishedOfBusiness(businessId, RESENAS_EN_FICHA);
        if (filas.isEmpty()) return List.of();

        // Los nombres en UNA llamada, no una por resena.
        Map<UUID, UUID> personaDeEmpleado = new HashMap<>();
        for (AppointmentReview r : filas) {
            employees.findById(r.getEmployeeId())
                    .ifPresent(e -> personaDeEmpleado.put(r.getEmployeeId(), e.getThirdPartyId()));
        }
        Map<String, String> nombres = personaDeEmpleado.isEmpty()
                ? Map.of()
                : nombresDe(Set.copyOf(personaDeEmpleado.values()));

        List<ReviewView> out = new ArrayList<>(filas.size());
        for (AppointmentReview r : filas) {
            UUID persona = personaDeEmpleado.get(r.getEmployeeId());
            String nombre = persona == null ? null : nombres.get(persona.toString());
            out.add(new ReviewView(
                    r.getBusinessStars(), r.getEmployeeStars(), r.getComment(),
                    nombre == null || nombre.isBlank() ? "El equipo" : nombre,
                    r.getBusinessReply(),
                    r.getCreatedDate() == null ? null : r.getCreatedDate().toLocalDate().toString()));
        }
        return out;
    }

    private Map<String, String> nombresDe(Set<UUID> personas) {
        try {
            return people.personNames(personas);
        } catch (RuntimeException ex) {
            // Sin nombres la ficha sigue teniendo sus opiniones: se pierde el
            // "te atendio Ana", no la resena.
            return Map.of();
        }
    }

    /** La media a una decima, o {@code null} si todavia no la califico nadie. */
    private static BigDecimal media(Integer resenas, Integer estrellas) {
        if (resenas == null || resenas == 0) return null;
        return BigDecimal.valueOf(estrellas)
                .divide(BigDecimal.valueOf(resenas), 1, java.math.RoundingMode.HALF_UP);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static CardView toCard(JpaMarketplaceRepository.Card c) {
        return new CardView(
                c.getSlug(), c.getName(), c.getHeadline(), c.getCategoryCode(),
                c.getCoverImageUrl(), c.getLogoUrl(), c.getCity(), c.getAddressLine(),
                c.getReviewCount(), media(c.getReviewCount(), c.getStarSum()),
                Boolean.TRUE.equals(c.getIsVerified()),
                c.getServiceCount(), c.getMinPrice(),
                c.getLatitude(), c.getLongitude());
    }
}
