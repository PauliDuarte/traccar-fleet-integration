package org.example.fleet.delivery.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PedidoDomainEvent(
    long id,
    @JsonProperty("messageId") String messageId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("pedido_id") String pedidoId,
    @JsonProperty("device_id") String deviceId,
    @JsonProperty("estado_anterior") String estadoAnterior,
    @JsonProperty("estado_nuevo") String estadoNuevo,
    String timestamp,
    @JsonProperty("distancia_destino_m") Double distanciaDestinoM
) {
}
