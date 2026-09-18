package org.example.fleet.delivery.model;

import java.time.Instant;

public record PedidoEvento(
    long id,
    String pedidoId,
    String hito,
    String detalle,
    boolean notificado,
    Instant timestamp
) {
}
