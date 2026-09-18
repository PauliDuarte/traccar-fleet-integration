package org.example.fleet.delivery.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingPushGateway implements PushGateway {
    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    @Override
    public boolean send(PushNotification notification) {
        log.info("[PUSH SIMULADO] pedido={}, hito={}", notification.pedidoId(), notification.hito());
        return true;
    }
}
