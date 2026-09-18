package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.notification.NotificationResult;
import org.example.fleet.delivery.notification.PushGateway;
import org.example.fleet.delivery.notification.PushNotification;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class NotificationRepository {
    private final DataSource dataSource;

    public NotificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public NotificationResult sendOnce(PedidoDomainEvent event, PushGateway gateway) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                String query = "SELECT e.id, e.notificado, e.hito, p.cliente_fcm_id "
                    + "FROM pedido_eventos e JOIN pedidos p ON p.id = e.pedido_id "
                    + "WHERE e.message_id = ? AND e.pedido_id = ? AND e.hito = ? FOR UPDATE";
                long eventId;
                String fcmId;
                try (var statement = connection.prepareStatement(query)) {
                    statement.setString(1, event.messageId());
                    statement.setString(2, event.pedidoId());
                    statement.setString(3, event.estadoNuevo());
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return NotificationResult.NOT_FOUND;
                        }
                        if (result.getBoolean("notificado")) {
                            connection.rollback();
                            return NotificationResult.ALREADY_SENT;
                        }
                        eventId = result.getLong("id");
                        fcmId = result.getString("cliente_fcm_id");
                    }
                }
                boolean sent;
                try {
                    sent = gateway.send(new PushNotification(event.pedidoId(), event.estadoNuevo(), fcmId));
                } catch (Exception exception) {
                    sent = false;
                }
                if (!sent) {
                    connection.rollback();
                    return NotificationResult.FAILED;
                }
                try (var statement = connection.prepareStatement(
                    "UPDATE pedido_eventos SET notificado = TRUE WHERE id = ? AND notificado = FALSE")) {
                    statement.setLong(1, eventId);
                    statement.executeUpdate();
                }
                connection.commit();
                return NotificationResult.SENT;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }
}
