package org.example.fleet.delivery.repository;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class RepartidorRepository {
    private final DataSource dataSource;

    public RepartidorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsByDeviceId(String deviceId) throws SQLException {
        String sql = "SELECT 1 FROM repartidores WHERE device_id = ?";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, deviceId);
            try (var result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
