package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Solo lectura del catalogo {@code day_of_week}, que administra system-service.
 *
 * <p>Los horarios guardan el dia como FK al catalogo, y el motor de
 * disponibilidad necesita el numero ISO (1 = lunes). Son siete filas que no
 * cambian nunca: leerlas de la tabla —que esta en el mismo esquema— y
 * cachearlas cuesta menos que una llamada entre servicios en cada calculo.</p>
 *
 * <p>{@code @Immutable} deja claro que aqui no se escribe: quien administra el
 * catalogo es system-service.</p>
 */
@Getter @Setter
@Entity @Table(name = "day_of_week")
@Immutable
public class DayOfWeekRefEntity extends BaseEntity {
    @Column(name = "Code", length = 80, nullable = false) private String code;
    @Column(name = "Name", length = 120, nullable = false) private String name;
    /** El numero ISO del dia, como texto: "1" lunes ... "7" domingo. */
    @Column(name = "Value", length = 500) private String value;
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
}
