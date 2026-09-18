package org.example.fleet.delivery.service;

import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.example.fleet.model.VehiclePosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PositionTrackingServiceTest {
    private DataSource dataSource;
    private PedidoRepository pedidos;
    private PosicionRepository posiciones;
    private PositionTrackingService service;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = RepositoryTestSupport.createDatabase("tracking-" + System.nanoTime());
        pedidos = new PedidoRepository(dataSource);
        posiciones = new PosicionRepository(dataSource);
        service = new PositionTrackingService(pedidos, posiciones, new EventoRepository(dataSource));
    }

    @Test
    void correlatesPositionStoresItAndStartsOrder() throws Exception {
        insertPedido("PED-GPS-1");
        VehiclePosition position = position("MSG-1", "repartidor-01", true,
            "2026-09-02T12:01:00Z");

        assertEquals(PositionResult.STARTED, service.process(position));
        var stored = posiciones.findByPedidoId("PED-GPS-1").orElseThrow();
        assertEquals("MSG-1", stored.messageId());
        assertEquals(Instant.parse(position.timestamp()), stored.timestamp());
        assertEquals(PedidoEstado.EN_CAMINO, pedidos.findById("PED-GPS-1").orElseThrow().estado());
        assertEquals(1, countEvents("PED-GPS-1", "EN_CAMINO"));
    }

    @Test
    void repeatedPositionDoesNotDuplicateMilestone() throws Exception {
        insertPedido("PED-GPS-2");
        service.process(position("MSG-2", "repartidor-01", true, "2026-09-02T12:01:00Z"));
        service.process(position("MSG-2", "repartidor-01", true, "2026-09-02T12:01:00Z"));
        assertEquals(1, countEvents("PED-GPS-2", "EN_CAMINO"));
    }

    @Test
    void ignoresDriverWithoutActiveOrder() throws Exception {
        assertEquals(PositionResult.NO_ACTIVE_ORDER,
            service.process(position("MSG-3", "repartidor-01", true, "2026-09-02T12:01:00Z")));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        assertEquals(PositionResult.INVALID,
            service.process(position("MSG-4", "repartidor-01", false, "2026-09-02T12:01:00Z")));
        assertEquals(PositionResult.INVALID,
            service.process(position("MSG-5", "repartidor-01", true, "not-a-timestamp")));
    }

    private void insertPedido(String id) throws Exception {
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido(id, "Juan", "+595972222222", null, "Asunción",
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now));
    }

    private int countEvents(String pedidoId, String hito) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM pedido_eventos WHERE pedido_id = ? AND hito = ?")) {
            statement.setString(1, pedidoId);
            statement.setString(2, hito);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static VehiclePosition position(String messageId, String deviceId, boolean valid, String timestamp) {
        return new VehiclePosition("1.0", messageId, deviceId, timestamp,
            -25.2971, -57.6362, 18.4, 90, valid, Map.of());
    }
}
