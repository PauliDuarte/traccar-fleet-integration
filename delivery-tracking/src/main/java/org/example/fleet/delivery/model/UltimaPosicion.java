package org.example.fleet.delivery.model;

import java.time.Instant;

public record UltimaPosicion(
    String pedidoId,
    String deviceId,
    String messageId,
    double lat,
    double lon,
    double velocidadKmh,
    double distanciaDestinoM,
    Instant timestamp
) {
}
