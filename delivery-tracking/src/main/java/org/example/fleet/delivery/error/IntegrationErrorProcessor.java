package org.example.fleet.delivery.error;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.model.VehiclePosition;

import java.time.Instant;

public final class IntegrationErrorProcessor implements Processor {
    @Override
    public void process(Exchange exchange) {
        Throwable cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        Object body = exchange.getMessage().getBody();
        String messageId = header(exchange, "X-Correlation-Message-Id");
        String pedidoId = header(exchange, "X-Correlation-Pedido-Id");
        String deviceId = header(exchange, "X-Correlation-Device-Id");
        if (body instanceof PedidoDomainEvent event) {
            messageId = event.messageId();
            pedidoId = event.pedidoId();
            deviceId = event.deviceId();
        } else if (body instanceof VehiclePosition position) {
            messageId = position.messageId();
            deviceId = position.deviceId();
        }
        String type = isInvalidMessage(cause) ? "MENSAJE_INVALIDO" : "ERROR_TECNICO";
        String reason = cause == null ? "UnknownFailure" : cause.getClass().getSimpleName();
        exchange.getMessage().setBody(new IntegrationError(reason, messageId, pedidoId, deviceId,
            Instant.now().toString(), type, header(exchange, "X-Error-Origin")));
        exchange.getMessage().removeHeaders("*", "Content-Type");
        exchange.getMessage().setHeader("Content-Type", "application/json");
    }

    private static boolean isInvalidMessage(Throwable cause) {
        if (cause == null) {
            return false;
        }
        String name = cause.getClass().getName();
        return name.contains("Json") || name.contains("MismatchedInput") || name.contains("InvalidFormat");
    }

    private static String header(Exchange exchange, String name) {
        return exchange.getMessage().getHeader(name, String.class);
    }
}
