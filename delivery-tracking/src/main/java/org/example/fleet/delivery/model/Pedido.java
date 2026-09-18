package org.example.fleet.delivery.model;

import java.time.Instant;

public record Pedido(
    String id,
    String clienteNombre,
    String clienteMsisdn,
    String clienteFcmId,
    String direccionTexto,
    double latDestino,
    double lonDestino,
    int radioLlegadaM,
    String repartidorDeviceId,
    PedidoEstado estado,
    Instant fechaCreacion,
    Instant fechaActualizacion
) {
}
