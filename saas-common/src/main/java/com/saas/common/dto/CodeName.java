package com.saas.common.dto;

/**
 * Par {@code code} + {@code name} (valor legible) de una referencia resuelta
 * (catálogo o ubicación). Se usa para DENORMALIZAR el read model: además del
 * Id, el documento/respuesta lleva el código (estable, bueno para comparar) y
 * el nombre (legible, bueno para mostrar) — así el front no tiene que mapear Ids.
 */
public record CodeName(String code, String name) {}
