package org.example.fleet.delivery.repository;

import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.model.UltimaPosicion;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosicionRepositoryTest {
    @Test
    void upsertsLatestPositionAndKeepsMessageId() throws Exception {
        var dataSource = RepositoryTestSupport.createDatabase("position-" + System.nanoTime());
        var pedidos = new PedidoRepository(dataSource);
        Instant created = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido("PED-3", "Juan", "+595972222222", null, null,
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, created, created));
        var posiciones = new PosicionRepository(dataSource);

        posiciones.upsert(new UltimaPosicion("PED-3", "repartidor-01", "MSG-1",
            -25.30, -57.64, 10, 500, created.plusSeconds(10)));
        posiciones.upsert(new UltimaPosicion("PED-3", "repartidor-01", "MSG-2",
            -25.29, -57.63, 12, 200, created.plusSeconds(20)));

        var actual = posiciones.findByPedidoId("PED-3").orElseThrow();
        assertEquals("MSG-2", actual.messageId());
        assertEquals(200, actual.distanciaDestinoM());
    }
}
