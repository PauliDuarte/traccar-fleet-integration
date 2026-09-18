package org.example.fleet.delivery.repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class EventoRepository {
    private final DataSource dataSource;

    public EventoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean insertIfAbsent(String pedidoId, String hito, String detalle, Instant timestamp)
        throws SQLException {
        String sql = "INSERT INTO pedido_eventos (pedido_id, hito, detalle, timestamp) "
            + "VALUES (?, ?, CAST(? AS JSONB), ?) ON CONFLICT (pedido_id, hito) DO NOTHING";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, pedidoId);
            statement.setString(2, hito);
            statement.setString(3, detalle);
            statement.setTimestamp(4, Timestamp.from(timestamp));
            return statement.executeUpdate() == 1;
        }
    }
}
