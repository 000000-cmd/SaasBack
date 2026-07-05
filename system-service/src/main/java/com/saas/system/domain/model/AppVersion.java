package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Version del APK publicada por el administrador. El binario vive en disco
 * (app.apk.storage-dir); aqui la metadata para el historico, la verificacion
 * de integridad (checksum) y el control de actualizacion del APK.
 *
 * <p>Solo UNA version esta vigente ({@code isCurrent}). Publicar exige
 * {@code versionCode} creciente: es lo que Android usa para actualizar EN SITIO
 * (misma firma + code mayor) conservando la data local del usuario.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppVersion extends BaseDomain {

    /** Version semantica visible (p.ej. 1.0.1). Unica. */
    private String version;

    /** versionCode Android. Unico y creciente entre publicaciones. */
    private Integer versionCode;

    /** Nombre del archivo en el storage (saas-app-<version>.apk). */
    private String fileName;

    /** SHA-256 del binario (el APK lo verifica antes de instalar). */
    private String checksum;

    private Long sizeBytes;

    /** Notas de la version (visibles en el modal de actualizacion). */
    private String notes;

    /** Version vigente: la que el APK exige y la que sirve el link publico. */
    private Boolean isCurrent;
}
