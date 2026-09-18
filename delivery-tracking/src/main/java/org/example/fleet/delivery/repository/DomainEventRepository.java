package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.events.PedidoDomainEvent;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DomainEventRepository {
    private final DataSource dataSource;

    public DomainEventRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PedidoDomainEvent> findUnpublished(int limit) throws SQLException {
        String sql = "SELECT id, message_id, event_type, pedido_id, device_id, estado_anterior, "
            + "estado_nuevo, timestamp, distancia_destino_m FROM pedido_eventos "
            + "WHERE publicado = FALSE AND message_id IS NOT NULL ORDER BY id LIMIT ?";
        List<PedidoDomainEvent> events = new ArrayList<>();
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(new PedidoDomainEvent(result.getLong("id"), result.getString("message_id"),
                        result.getString("event_type"), result.getString("pedido_id"),
                        result.getString("device_id"), result.getString("estado_anterior"),
                        result.getString("estado_nuevo"), result.getTimestamp("timestamp").toInstant().toString(),
                        result.getObject("distancia_destino_m", Double.class)));
                }
            }
        }
        return events;
    }

    public void markPublished(long id) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("UPDATE pedido_eventos SET publicado = TRUE WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
