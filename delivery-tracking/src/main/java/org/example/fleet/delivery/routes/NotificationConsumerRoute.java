package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;
import org.example.fleet.delivery.DeliveryRuntime;
import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.notification.LoggingPushGateway;
import org.example.fleet.delivery.notification.NotificationResult;
import org.example.fleet.delivery.notification.NotificationService;
import org.example.fleet.delivery.notification.PushDeliveryException;
import org.example.fleet.delivery.repository.NotificationRepository;

public class NotificationConsumerRoute extends RouteBuilder {
    private final NotificationService service;
    private final String source;

    public NotificationConsumerRoute() {
        this(new NotificationService(new NotificationRepository(DeliveryRuntime.dataSource()),
            new LoggingPushGateway()), "amqp:queue:notification.commands");
    }

    public NotificationConsumerRoute(NotificationService service, String source) {
        this.service = service;
        this.source = source;
    }

    @Override
    public void configure() {
        from(source)
            .routeId("delivery-push-notifications")
            .unmarshal().json(PedidoDomainEvent.class)
            .filter(exchange -> {
                String state = exchange.getMessage().getBody(PedidoDomainEvent.class).estadoNuevo();
                return "EN_CAMINO".equals(state) || "CERCA".equals(state) || "ENTREGADO".equals(state);
            })
                .process(exchange -> {
                    NotificationResult result = service.notify(
                        exchange.getMessage().getBody(PedidoDomainEvent.class));
                    if (result == NotificationResult.FAILED) {
                        throw new PushDeliveryException("Simulated push delivery failed");
                    }
                    exchange.getMessage().setHeader("X-Notification-Result", result.name());
                })
            .end();
    }
}
