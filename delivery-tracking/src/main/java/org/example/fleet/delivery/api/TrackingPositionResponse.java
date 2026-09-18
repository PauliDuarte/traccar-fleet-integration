package org.example.fleet.delivery.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TrackingPositionResponse(
    double lat,
    double lon,
    @JsonProperty("velocidad_kmh") double velocidadKmh,
    @JsonProperty("distancia_destino_m") double distanciaDestinoM,
    Instant timestamp
) {
}
