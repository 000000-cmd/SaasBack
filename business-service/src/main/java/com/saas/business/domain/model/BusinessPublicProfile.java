package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * La ficha del negocio EN EL DIRECTORIO.
 *
 * <h3>Aqui NO esta lo que describe al negocio</h3>
 * <p>Ni el titular, ni la descripcion, ni la portada, ni la categoria, ni la
 * direccion. Todo eso el dueño ya lo escribio en otra parte —al crear el
 * negocio, en «Mi pagina» y en «Sedes»— y el directorio lo LEE de alli. Tener
 * aqui una segunda copia significaba que cambiar la direccion en Sedes dejaba
 * al directorio enseñando la vieja, sin que nada fallara.</p>
 *
 * <p>Lo que queda es lo unico que existe por el directorio y por nada mas: si
 * quiere aparecer, si esta verificado, y como lo califican.</p>
 *
 * <h3>Aparecer es opt-in</h3>
 * <p>{@code isListed} nace en falso. No todos los negocios quieren estar en un
 * directorio publico, y meterlos por defecto seria publicar sus datos sin que
 * lo hayan pedido.</p>
 *
 * <h3>Por que se guardan la suma y el conteo, y no la media</h3>
 * <p>Con la media habria que leerla, calcular la nueva y escribirla — y dos
 * resenas a la vez se pisarian. Con {@code StarSum} y {@code ReviewCount} la
 * actualizacion es {@code SET StarSum = StarSum + :estrellas}, que la base
 * resuelve atomicamente. La media se divide al leer.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessPublicProfile extends BaseDomain {

    private UUID businessId;

    @Builder.Default private Boolean isListed = Boolean.FALSE;
    @Builder.Default private Boolean isVerified = Boolean.FALSE;
    @Builder.Default private Integer reviewCount = 0;
    @Builder.Default private Integer starSum = 0;

    /** La media, o {@code null} si todavia no lo ha calificado nadie. */
    public BigDecimal average() {
        if (reviewCount == null || reviewCount == 0) return null;
        return BigDecimal.valueOf(starSum)
                .divide(BigDecimal.valueOf(reviewCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
