package org.example.fleet.delivery.notification;

import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.repository.NotificationRepository;

import java.sql.SQLException;
import java.util.Set;

public final class NotificationService {
    private static final Set<String> NOTIFIABLE = Set.of("EN_CAMINO", "CERCA", "ENTREGADO");
    private final NotificationRepository repository;
    private final PushGateway gateway;

    public NotificationService(NotificationRepository repository, PushGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    public NotificationResult notify(PedidoDomainEvent event) throws SQLException {
        if (event == null || !NOTIFIABLE.contains(event.estadoNuevo())) {
            return NotificationResult.IGNORED;
        }
        return repository.sendOnce(event, gateway);
    }
}
