package com.saas.common.mapper;

import org.mapstruct.Builder;
import org.mapstruct.MapperConfig;

/**
 * Configuracion central para todos los mappers MapStruct del proyecto.
 *
 * Importante: {@code disableBuilder = true} fuerza a MapStruct a usar
 * NoArgsConstructor + setters en lugar del builder de Lombok. Esto es
 * necesario porque Lombok {@code @Builder} solo expone los campos
 * declarados en la propia clase (no los heredados de {@code BaseEntity} /
 * {@code BaseDomain}), por lo que los mappers fallan al referenciar
 * {@code Id}, {@code AuditUser}, etc. via {@code @Mapping(target=...)}.
 *
 * Al usar setters, los campos heredados se mapean correctamente.
 *
 * Uso:
 *   {@code @Mapper(config = BaseMapStructConfig.class)}
 */
@MapperConfig(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true)
)
public interface BaseMapStructConfig {
}
