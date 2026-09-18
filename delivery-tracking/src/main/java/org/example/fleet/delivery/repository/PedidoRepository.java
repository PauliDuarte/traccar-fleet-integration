package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public final class PedidoRepository {
    private final DataSource dataSource;

    public PedidoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Pedido> findById(String id) throws SQLException {
        return findOne("SELECT * FROM pedidos WHERE id = ?", id);
    }

    public Optional<Pedido> findActiveByDeviceId(String deviceId) throws SQLException {
        return findOne("SELECT * FROM pedidos WHERE repartidor_device_id = ? "
            + "AND estado NOT IN ('ENTREGADO', 'CANCELADO')", deviceId);
    }

    public void insert(Pedido pedido) throws SQLException {
        String sql = "INSERT INTO pedidos (id, cliente_nombre, cliente_msisdn, cliente_fcm_id, "
            + "direccion_texto, lat_destino, lon_destino, radio_llegada_m, repartidor_device_id, "
            + "estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, pedido.id());
            statement.setString(2, pedido.clienteNombre());
            statement.setString(3, pedido.clienteMsisdn());
            statement.setString(4, pedido.clienteFcmId());
            statement.setString(5, pedido.direccionTexto());
            statement.setDouble(6, pedido.latDestino());
            statement.setDouble(7, pedido.lonDestino());
            statement.setInt(8, pedido.radioLlegadaM());
            statement.setString(9, pedido.repartidorDeviceId());
            statement.setString(10, pedido.estado().name());
            statement.setTimestamp(11, Timestamp.from(pedido.fechaCreacion()));
            statement.setTimestamp(12, Timestamp.from(pedido.fechaActualizacion()));
            statement.executeUpdate();
        }
    }

    public boolean updateEstado(String id, PedidoEstado expected, PedidoEstado next, Instant timestamp)
        throws SQLException {
        String sql = "UPDATE pedidos SET estado = ?, fecha_actualizacion = ? WHERE id = ? AND estado = ?";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, next.name());
            statement.setTimestamp(2, Timestamp.from(timestamp));
            statement.setString(3, id);
            statement.setString(4, expected.name());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean transitionWithEvent(String id, PedidoEstado expected, PedidoEstado next,
                                       String detail, Instant timestamp) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                String update = "UPDATE pedidos SET estado = ?, fecha_actualizacion = ? "
                    + "WHERE id = ? AND estado = ?";
                int updated;
                try (var statement = connection.prepareStatement(update)) {
                    statement.setString(1, next.name());
                    statement.setTimestamp(2, Timestamp.from(timestamp));
                    statement.setString(3, id);
                    statement.setString(4, expected.name());
                    updated = statement.executeUpdate();
                }
                if (updated == 0) {
                    connection.rollback();
                    return false;
                }
                String jsonType = connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")
                    ? "JSONB" : "JSON";
                String insertEvent = "INSERT INTO pedido_eventos (pedido_id, hito, detalle, timestamp) "
                    + "VALUES (?, ?, CAST(? AS " + jsonType + "), ?)";
                try (var statement = connection.prepareStatement(insertEvent)) {
                    statement.setString(1, id);
                    statement.setString(2, next.name());
                    statement.setString(3, detail);
                    statement.setTimestamp(4, Timestamp.from(timestamp));
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                if ("23505".equals(exception.getSQLState())) {
                    return false;
                }
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private Optional<Pedido> findOne(String sql, String value) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(new Pedido(
                    result.getString("id"), result.getString("cliente_nombre"),
                    result.getString("cliente_msisdn"), result.getString("cliente_fcm_id"),
                    result.getString("direccion_texto"), result.getDouble("lat_destino"),
                    result.getDouble("lon_destino"), result.getInt("radio_llegada_m"),
                    result.getString("repartidor_device_id"), PedidoEstado.valueOf(result.getString("estado")),
                    result.getTimestamp("fecha_creacion").toInstant(),
                    result.getTimestamp("fecha_actualizacion").toInstant())) : Optional.empty();
            }
        }
    }
}
