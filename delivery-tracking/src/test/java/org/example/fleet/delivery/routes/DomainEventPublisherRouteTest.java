package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.repository.DomainEventRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainEventPublisherRouteTest extends CamelTestSupport {
    private DomainEventRepository events;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var dataSource = RepositoryTestSupport.createDatabase("domain-events-" + System.nanoTime());
        var pedidos = new PedidoRepository(dataSource);
        events = new DomainEventRepository(dataSource);
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido("PED-EVENT", "Juan", "+595972222222", null, null,
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now));
        pedidos.transitionWithEvent("PED-EVENT", "repartidor-01", PedidoEstado.RECIBIDO,
            PedidoEstado.EN_CAMINO, 500, "{}", now.plusSeconds(10));
        pedidos.transitionWithEvent("PED-EVENT", "repartidor-01", PedidoEstado.EN_CAMINO,
            PedidoEstado.CERCA, 100, "{}", now.plusSeconds(20));
        pedidos.transitionWithEvent("PED-EVENT", "repartidor-01", PedidoEstado.CERCA,
            PedidoEstado.ENTREGADO, 50, "{}", now.plusSeconds(30));
        return new DomainEventPublisherRoute(events, "direct:publish-events",
            "mock:pedido-events", "mock:notification-commands");
    }

    @Test
    void publishesEachOutboxEventOnceToBothChannels() throws Exception {
        MockEndpoint domain = getMockEndpoint("mock:pedido-events");
        MockEndpoint notifications = getMockEndpoint("mock:notification-commands");
        domain.expectedMessageCount(3);
        notifications.expectedMessageCount(3);
        domain.expectedHeaderValuesReceivedInAnyOrder("JMSType",
            "pedido.estado-cambiado", "pedido.cerca", "pedido.entregado");

        template.sendBody("direct:publish-events", null);
        template.sendBody("direct:publish-events", null);

        MockEndpoint.assertIsSatisfied(context);
        assertTrue(events.findUnpublished(10).isEmpty());
    }
}
