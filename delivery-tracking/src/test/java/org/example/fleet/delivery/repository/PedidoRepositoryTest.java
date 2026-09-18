package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PedidoRepositoryTest {
    private DataSource dataSource;
    private PedidoRepository pedidos;
    private RepartidorRepository repartidores;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = RepositoryTestSupport.createDatabase("pedidos-" + System.nanoTime());
        pedidos = new PedidoRepository(dataSource);
        repartidores = new RepartidorRepository(dataSource);
    }

    @Test
    void findsExistingRepartidor() throws Exception {
        assertTrue(repartidores.existsByDeviceId("repartidor-01"));
        assertFalse(repartidores.existsByDeviceId("desconocido"));
    }

    @Test
    void insertsAndFindsPedido() throws Exception {
        Pedido pedido = pedido("PED-1");
        pedidos.insert(pedido);

        assertEquals(pedido, pedidos.findById("PED-1").orElseThrow());
        assertEquals("PED-1", pedidos.findActiveByDeviceId("repartidor-01").orElseThrow().id());
    }

    @Test
    void updatesEstadoOnlyFromExpectedState() throws Exception {
        pedidos.insert(pedido("PED-2"));

        assertTrue(pedidos.updateEstado("PED-2", PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, Instant.now()));
        assertFalse(pedidos.updateEstado("PED-2", PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, Instant.now()));
        assertEquals(PedidoEstado.EN_CAMINO, pedidos.findById("PED-2").orElseThrow().estado());
    }

    @Test
    void rollsBackStateWhenMilestoneCannotBeInserted() throws Exception {
        pedidos.insert(pedido("PED-ATOMIC"));
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "INSERT INTO pedido_eventos (pedido_id, hito, detalle, timestamp) VALUES (?, ?, '{}', ?)")) {
            statement.setString(1, "PED-ATOMIC");
            statement.setString(2, "EN_CAMINO");
            statement.setTimestamp(3, java.sql.Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }

        assertFalse(pedidos.transitionWithEvent("PED-ATOMIC", "repartidor-01", PedidoEstado.RECIBIDO,
            PedidoEstado.EN_CAMINO, 100, "{}", Instant.now()));
        assertEquals(PedidoEstado.RECIBIDO, pedidos.findById("PED-ATOMIC").orElseThrow().estado());
    }

    private static Pedido pedido(String id) {
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        return new Pedido(id, "Juan", "+595972222222", "fcm", "Asunción",
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now);
    }
}
