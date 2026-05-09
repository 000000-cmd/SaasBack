package com.saas.search.infrastructure.kafka;

import com.saas.common.events.EventEnvelope;

/**
 * Contrato de un handler de eventos. Cada handler decide que tipos de evento
 * procesa via {@link #supports(String)} y como procesarlos via
 * {@link #handle(EventEnvelope)}.
 *
 * Spring inyecta automaticamente todos los beans que implementan esta
 * interfaz al {@code DomainEventListener}, que dispatcha cada evento al
 * primer handler que lo soporte.
 *
 * Para agregar un nuevo handler:
 *
 *   Crear la clase con {@code @Component} que implementa esta interfaz.
 *   Definir {@code supports} para los types relevantes.
 *   Implementar {@code handle} con la logica de indexacion.
 *
 * Cero cambios en el listener.
 */
public interface EventHandler {

    /**
     * @param eventType el campo {@code type} del envelope (ej. "user.created").
     * @return true si este handler procesa este tipo.
     */
    boolean supports(String eventType);

    /**
     * Procesa el evento. Si tira excepcion, el listener no confirma el offset
     * y Kafka reentrega el mensaje (hasta {@code max-retries}).
     */
    void handle(EventEnvelope envelope);
}