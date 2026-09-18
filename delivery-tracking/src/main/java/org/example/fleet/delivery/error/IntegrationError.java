package org.example.fleet.delivery.error;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntegrationError(
    String motivo,
    @JsonProperty("message_id") String messageId,
    @JsonProperty("pedido_id") String pedidoId,
    @JsonProperty("device_id") String deviceId,
    String timestamp,
    String tipo,
    String origen
) {
}
