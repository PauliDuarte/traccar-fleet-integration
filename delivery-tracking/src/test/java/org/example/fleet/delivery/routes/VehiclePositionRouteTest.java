package org.example.fleet.delivery.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.example.fleet.delivery.service.PositionTrackingService;
import org.example.fleet.model.VehiclePosition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VehiclePositionRouteTest extends CamelTestSupport {
    private PosicionRepository posiciones;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var dataSource = RepositoryTestSupport.createDatabase("position-route-" + System.nanoTime());
        var pedidos = new PedidoRepository(dataSource);
        posiciones = new PosicionRepository(dataSource);
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido("PED-ROUTE", "Juan", "+595972222222", null, null,
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now));
        var service = new PositionTrackingService(pedidos, posiciones);
        return new VehiclePositionRoute(service, "direct:test-positions");
    }

    @Test
    void setsCorrelationHeadersAndProcessesCanonicalPosition() throws Exception {
        VehiclePosition position = position("MSG-ROUTE", true);
        Exchange exchange = template.request("direct:test-positions", value ->
            value.getMessage().setBody(new ObjectMapper().writeValueAsString(position)));

        assertEquals("repartidor-01", exchange.getMessage().getHeader("X-Correlation-Device-Id"));
        assertEquals("MSG-ROUTE", exchange.getMessage().getHeader("X-Correlation-Message-Id"));
        assertEquals("MSG-ROUTE", posiciones.findByPedidoId("PED-ROUTE").orElseThrow().messageId());
    }

    @Test
    void filtersInvalidCanonicalPosition() throws Exception {
        VehiclePosition position = position("MSG-INVALID", false);
        template.request("direct:test-positions", value ->
            value.getMessage().setBody(new ObjectMapper().writeValueAsString(position)));
        assertTrue(posiciones.findByPedidoId("PED-ROUTE").isEmpty());
    }

    private static VehiclePosition position(String messageId, boolean valid) {
        return new VehiclePosition("1.0", messageId, "repartidor-01", "2026-09-02T12:01:00Z",
            -25.2971, -57.6362, 18.4, 90, valid, Map.of());
    }
}
