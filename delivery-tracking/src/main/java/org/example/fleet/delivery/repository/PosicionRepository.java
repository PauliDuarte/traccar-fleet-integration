package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.model.UltimaPosicion;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public final class PosicionRepository {
    private final DataSource dataSource;

    public PosicionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void upsert(UltimaPosicion posicion) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                String update = "UPDATE pedido_ultima_posicion SET device_id = ?, message_id = ?, lat = ?, "
                    + "lon = ?, velocidad_kmh = ?, distancia_destino_m = ?, timestamp = ? "
                    + "WHERE pedido_id = ? AND timestamp <= ?";
                int updated;
                try (var statement = connection.prepareStatement(update)) {
                    bindPosition(statement, posicion, false);
                    updated = statement.executeUpdate();
                }
                if (updated == 0 && !exists(connection, posicion.pedidoId())) {
                    String insert = "INSERT INTO pedido_ultima_posicion "
                        + "(device_id, message_id, lat, lon, velocidad_kmh, distancia_destino_m, timestamp, pedido_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (var statement = connection.prepareStatement(insert)) {
                        bindPosition(statement, posicion, true);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private static void bindPosition(java.sql.PreparedStatement statement, UltimaPosicion posicion,
                                     boolean insert) throws SQLException {
        statement.setString(1, posicion.deviceId());
        statement.setString(2, posicion.messageId());
        statement.setDouble(3, posicion.lat());
        statement.setDouble(4, posicion.lon());
        statement.setDouble(5, posicion.velocidadKmh());
        statement.setDouble(6, posicion.distanciaDestinoM());
        statement.setTimestamp(7, Timestamp.from(posicion.timestamp()));
        statement.setString(8, posicion.pedidoId());
        if (!insert) {
            statement.setTimestamp(9, Timestamp.from(posicion.timestamp()));
        }
    }

    private static boolean exists(java.sql.Connection connection, String pedidoId) throws SQLException {
        try (var statement = connection.prepareStatement(
            "SELECT 1 FROM pedido_ultima_posicion WHERE pedido_id = ?")) {
            statement.setString(1, pedidoId);
            try (var result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public Optional<UltimaPosicion> findByPedidoId(String pedidoId) throws SQLException {
        String sql = "SELECT * FROM pedido_ultima_posicion WHERE pedido_id = ?";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, pedidoId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(new UltimaPosicion(
                    result.getString("pedido_id"), result.getString("device_id"),
                    result.getString("message_id"), result.getDouble("lat"), result.getDouble("lon"),
                    result.getDouble("velocidad_kmh"), result.getDouble("distancia_destino_m"),
                    result.getTimestamp("timestamp").toInstant())) : Optional.empty();
            }
        }
    }
}
