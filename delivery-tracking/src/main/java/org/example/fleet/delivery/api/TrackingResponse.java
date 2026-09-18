package org.example.fleet.delivery.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.fleet.delivery.model.PedidoEstado;

public record TrackingResponse(
    @JsonProperty("pedido_id") String pedidoId,
    PedidoEstado estado,
    TrackingPositionResponse posicion
) {
}
