package org.example.fleet.delivery.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.notification.NotificationService;
import org.example.fleet.delivery.repository.DomainEventRepository;
import org.example.fleet.delivery.repository.NotificationRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationConsumerRouteTest extends CamelTestSupport {
    private PedidoDomainEvent event;
    private javax.sql.DataSource dataSource;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        dataSource = RepositoryTestSupport.createDatabase("notification-route-" + System.nanoTime());
        var pedidos = new PedidoRepository(dataSource);
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido("PED-PUSH-ERROR", "Juan", "+595972222222", "fcm", null,
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now));
        pedidos.transitionWithEvent("PED-PUSH-ERROR", "repartidor-01", PedidoEstado.RECIBIDO,
            PedidoEstado.EN_CAMINO, 500, "{}", now.plusSeconds(10));
        event = new DomainEventRepository(dataSource).findUnpublished(1).getFirst();
        var service = new NotificationService(new NotificationRepository(dataSource), notification -> false);
        return new NotificationConsumerRoute(service, "direct:test-notifications", "mock:integration-errors");
    }

    @Test
    void exhaustedPushFailureGoesToErrorChannelAndRemainsRetryable() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:integration-errors");
        errors.expectedMessageCount(1);
        template.sendBody("direct:test-notifications", new ObjectMapper().writeValueAsString(event));
        MockEndpoint.assertIsSatisfied(context);
        var error = errors.getExchanges().getFirst().getMessage()
            .getBody(org.example.fleet.delivery.error.IntegrationError.class);
        assertEquals("ERROR_TECNICO", error.tipo());
        assertEquals(event.messageId(), error.messageId());
        assertEquals("notification.commands", error.origen());
        assertFalse(isNotified());
    }

    private boolean isNotified() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT notificado FROM pedido_eventos WHERE message_id = ?")) {
            statement.setString(1, event.messageId());
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }
}
