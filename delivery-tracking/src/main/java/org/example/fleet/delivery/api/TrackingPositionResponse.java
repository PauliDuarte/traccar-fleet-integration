package org.example.fleet.delivery.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrackingPositionResponse(
    double lat,
    double lon,
    @JsonProperty("velocidad_kmh") double velocidadKmh,
    @JsonProperty("distancia_destino_m") double distanciaDestinoM,
    String timestamp
) {
}
