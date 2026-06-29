package com.saas.thirdparty.domain.model;

import com.saas.common.model.BaseDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tercero: informacion base de una PERSONA NATURAL. De aqui derivan empleado,
 * dueno y cliente (en business-service, por referencia de UUID).
 *
 * <p>La identidad juridica (razon social, NIT) NO vive aqui: vive en {@code business}.
 * Email/telefono/redes viven en {@code third_party_contact}. Direcciones en
 * {@code third_party_address}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ThirdParty extends BaseDomain {

    /** FK al catalogo document_type (system-service). */
    private UUID documentTypeId;

    /** Numero de documento. Unico junto con documentTypeId. */
    private String documentNumber;

    /** FK opcional a app_user (auth-service). Null si el tercero no es usuario. */
    private UUID userId;

    private String firstName;
    private String secondName;
    private String firstLastName;
    private String secondLastName;

    /** FK al catalogo gender (system-service). */
    private UUID genderId;

    private LocalDate birthDate;

    private String photoUrl;

    public ThirdParty(@NotNull UUID uuid,
                      @NotBlank @Size(max = 40) String s, UUID uuid1, @NotBlank @Size(max = 80) String s1, @Size(max = 80) String s2, @NotBlank @Size(max = 80) String s3, @Size(max = 80) String s4, UUID uuid2, LocalDate localDate, @Size(max = 500) String s5, UUID businessId) {
        super();
    }
}
