package com.saas.business.infrastructure.controller;

import com.saas.business.domain.model.BusinessPublicProfile;
import com.saas.business.domain.port.out.IBusinessPublicProfileRepositoryPort;
import com.saas.business.infrastructure.persistence.repository.JpaMarketplaceRepository;
import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La ficha del negocio en el directorio, desde su panel.
 *
 * <h3>Aqui ya no se escribe nada del negocio</h3>
 * <p>Antes esta pantalla pedia otra vez el titular, la descripcion, la
 * categoria, la portada y la direccion — cosas que el dueño ya habia escrito al
 * crear el negocio, en «Mi pagina» y en «Sedes». Dos copias del mismo dato no se
 * mantienen solas: cambiaba la direccion en Sedes y el directorio seguia
 * enseñando la vieja.</p>
 *
 * <p>Ahora esto devuelve la ficha YA ARMADA con lo que el negocio tiene, dice
 * que le falta, y guarda una sola decision: aparecer o no.</p>
 *
 * <h3>Aparecer es una decision, no un efecto secundario</h3>
 * <p>{@code isListed} nace en falso y solo lo cambia el dueño. Publicar por
 * defecto seria poner los datos de un negocio en un directorio publico sin que
 * lo haya pedido — y quitarlo despues no deshace que estuvo.</p>
 *
 * <h3>Lo que el dueño NO puede tocar</h3>
 * <p>{@code isVerified} y los contadores de resenas. Lo primero lo otorga la
 * plataforma; lo segundo lo escriben las opiniones. Dejarlos editables seria
 * dar un boton para ponerse cinco estrellas.</p>
 */
@RestController
@RequestMapping("/public-profiles")
@RequiredArgsConstructor
public class PublicProfileController {

    private final IBusinessPublicProfileRepositoryPort profiles;
    private final JpaMarketplaceRepository directorio;

    /**
     * Lo que falta para que la ficha valga la pena.
     *
     * <p>Son CODIGOS, no frases: el texto y el enlace a la pantalla donde se
     * arregla son cosa de la interfaz. Devolver aqui «Ve a Mi pagina» ataria el
     * servidor a como se llaman hoy las pantallas.</p>
     */
    public enum Falta { HEADLINE, DESCRIPTION, COVER, LOGO, ADDRESS, LOCATION }

    /**
     * La ficha tal y como se publicaria, con lo que el negocio ya tiene.
     *
     * <p>Nada de esto se guarda aqui: sale de {@code business}, de su pagina y
     * de su sede. Es exactamente lo que veria un desconocido.</p>
     */
    public record ProfileView(
            UUID businessId, String slug, String name, String headline, String description,
            String categoryCode, String coverImageUrl, String logoUrl, String addressLine,
            BigDecimal latitude, BigDecimal longitude,
            List<Falta> missing,
            Boolean isListed, Boolean isVerified, Integer reviewCount, BigDecimal rating) {}

    /** Lo unico que esta pantalla decide. */
    public record ProfileRequest(@NotNull Boolean isListed) {}

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileView>> forBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(vista(businessId)));
    }

    /**
     * Aparecer o dejar de aparecer.
     *
     * <p>Para publicar hace falta al menos el titular. La comprobacion esta
     * AQUI y no solo en la pantalla: una ficha en blanco en un directorio
     * publico no le sirve a nadie, y la pantalla se puede saltar.</p>
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileView>> save(@RequestParam UUID businessId,
                                                         @Valid @RequestBody ProfileRequest req) {
        ProfileView actual = vista(businessId);

        if (Boolean.TRUE.equals(req.isListed()) && actual.missing().contains(Falta.HEADLINE)) {
            throw new BusinessException(
                    "Antes de aparecer en el directorio, escribe la frase de tu negocio en «Mi página».");
        }

        BusinessPublicProfile ficha = profiles.findByBusinessId(businessId)
                .orElseGet(() -> {
                    // Se crea desde el builder y no con `new`: con el
                    // constructor vacio los @Builder.Default no se aplican y
                    // los contadores entrarian nulos contra columnas NOT NULL.
                    BusinessPublicProfile nueva = BusinessPublicProfile.builder().build();
                    nueva.setBusinessId(businessId);
                    return profiles.save(nueva);
                });

        ficha.setIsListed(req.isListed());
        profiles.update(ficha);

        return ResponseEntity.ok(ApiResponse.success(
                vista(businessId),
                Boolean.TRUE.equals(req.isListed())
                        ? "Tu negocio ya aparece en el directorio"
                        : "Listo. Tu negocio no aparece en el directorio."));
    }

    // ----------------------------------------------------------- privados

    private ProfileView vista(UUID businessId) {
        JpaMarketplaceRepository.Detail d = directorio.previewOf(businessId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", businessId));

        boolean listado = profiles.findByBusinessId(businessId)
                .map(p -> Boolean.TRUE.equals(p.getIsListed()))
                .orElse(false);

        return new ProfileView(
                businessId, d.getSlug(), d.getName(), d.getHeadline(), d.getDescription(),
                d.getCategoryCode(), d.getCoverImageUrl(), d.getLogoUrl(), d.getAddressLine(),
                d.getLatitude(), d.getLongitude(),
                faltas(d),
                listado, Boolean.TRUE.equals(d.getIsVerified()),
                // Nulo = todavia no tiene ficha, y eso son cero resenas.
                cero(d.getReviewCount()), media(d.getReviewCount(), d.getStarSum()));
    }

    private static List<Falta> faltas(JpaMarketplaceRepository.Detail d) {
        List<Falta> out = new ArrayList<>();
        if (vacio(d.getHeadline())) out.add(Falta.HEADLINE);
        if (vacio(d.getDescription())) out.add(Falta.DESCRIPTION);
        if (vacio(d.getCoverImageUrl())) out.add(Falta.COVER);
        if (vacio(d.getLogoUrl())) out.add(Falta.LOGO);
        if (vacio(d.getAddressLine())) out.add(Falta.ADDRESS);
        // Sin coordenadas sale en el listado pero no en el mapa, y el mapa es
        // por donde entra quien busca "cerca de mi".
        if (d.getLatitude() == null || d.getLongitude() == null) out.add(Falta.LOCATION);
        return out;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }

    private static int cero(Integer n) { return n == null ? 0 : n; }

    private static BigDecimal media(Integer resenas, Integer estrellas) {
        if (resenas == null || resenas == 0) return null;
        return BigDecimal.valueOf(estrellas)
                .divide(BigDecimal.valueOf(resenas), 1, java.math.RoundingMode.HALF_UP);
    }
}
