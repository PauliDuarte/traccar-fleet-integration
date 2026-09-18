package org.example.fleet.delivery.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.fleet.delivery.model.PedidoEstado;

import java.time.Instant;

public record PedidoCreatedResponse(
    String id,
    PedidoEstado estado,
    @JsonProperty("fecha_creacion") Instant fechaCreacion
) {
}
