package org.example.fleet.delivery.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.example.fleet.delivery.api.ApiError;
import org.example.fleet.delivery.api.PedidoRequest;
import org.example.fleet.delivery.api.TrackingResponse;
import org.example.fleet.delivery.model.UltimaPosicion;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.delivery.repository.RepartidorRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.example.fleet.delivery.service.PedidoService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PedidoRestRouteTest extends CamelTestSupport {
    private PedidoRepository pedidos;
    private PosicionRepository posiciones;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var dataSource = RepositoryTestSupport.createDatabase("rest-" + System.nanoTime());
        pedidos = new PedidoRepository(dataSource);
        posiciones = new PosicionRepository(dataSource);
        var service = new PedidoService(pedidos, new RepartidorRepository(dataSource), posiciones,
            new EventoRepository(dataSource), Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC));
        return new PedidoRestRoute(service, false);
    }

    @Test
    void createsValidOrderWithAcceptedStatus() {
        Exchange exchange = create(valid("PED-REST-1"));
        assertEquals(202, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("PED-REST-1", pedidosUnchecked("PED-REST-1").id());
    }

    @Test
    void rejectsInvalidCoordinates() {
        PedidoRequest invalid = new PedidoRequest("PED-REST-2", "Juan", "+595972222222", null,
            null, 91.0, -57.0, 150, "repartidor-01");
        assertError(create(invalid), 400, "COORDENADAS_INVALIDAS");
    }

    @Test
    void rejectsUnknownDriver() {
        PedidoRequest invalid = new PedidoRequest("PED-REST-3", "Juan", "+595972222222", null,
            null, -25.0, -57.0, 150, "repartidor-99");
        assertError(create(invalid), 400, "REPARTIDOR_DESCONOCIDO");
    }

    @Test
    void rejectsDuplicateOrder() {
        create(valid("PED-REST-4"));
        assertError(create(valid("PED-REST-4")), 409, "PEDIDO_DUPLICADO");
    }

    @Test
    void returnsTrackingWithoutPosition() {
        create(valid("PED-REST-5"));
        TrackingResponse response = tracking("PED-REST-5").getMessage().getBody(TrackingResponse.class);
        assertNull(response.posicion());
    }

    @Test
    void returnsTrackingWithPosition() throws Exception {
        create(valid("PED-REST-6"));
        posiciones.upsert(new UltimaPosicion("PED-REST-6", "repartidor-01", "MSG-6",
            -25.2971, -57.6362, 18.4, 120, Instant.parse("2026-09-02T12:01:00Z")));
        TrackingResponse response = tracking("PED-REST-6").getMessage().getBody(TrackingResponse.class);
        assertNotNull(response.posicion());
        assertEquals(120, response.posicion().distanciaDestinoM());
    }

    @Test
    void returnsNotFoundForUnknownOrder() {
        assertError(tracking("NO-EXISTE"), 404, "PEDIDO_NO_ENCONTRADO");
    }

    private Exchange create(PedidoRequest request) {
        return template.request("direct:create-order", exchange -> exchange.getMessage().setBody(request));
    }

    private Exchange tracking(String id) {
        return template.request("direct:get-tracking", exchange -> exchange.getMessage().setHeader("id", id));
    }

    private org.example.fleet.delivery.model.Pedido pedidosUnchecked(String id) {
        try {
            return pedidos.findById(id).orElseThrow();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static PedidoRequest valid(String id) {
        return new PedidoRequest(id, "Juan Pérez", "+595972222222", "fcm_token_demo",
            "Av. España 1234", -25.2967, -57.6359, 150, "repartidor-01");
    }

    private static void assertError(Exchange exchange, int status, String code) {
        assertEquals(status, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(code, exchange.getMessage().getBody(ApiError.class).error());
    }
}
