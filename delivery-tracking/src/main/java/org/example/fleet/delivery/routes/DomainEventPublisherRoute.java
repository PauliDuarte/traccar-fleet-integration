package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;
import org.example.fleet.delivery.DeliveryRuntime;
import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.error.IntegrationErrorProcessor;
import org.example.fleet.delivery.repository.DomainEventRepository;

public class DomainEventPublisherRoute extends RouteBuilder {
    private final DomainEventRepository repository;
    private final String source;
    private final String eventDestination;
    private final String notificationDestination;
    private final String errorDestination;

    public DomainEventPublisherRoute() {
        this(new DomainEventRepository(DeliveryRuntime.dataSource()), "timer:pedido-outbox?period=1000",
            "amqp:topic:pedido.events", "amqp:queue:notification.commands", "direct:integration-errors");
    }

    public DomainEventPublisherRoute(DomainEventRepository repository, String source,
                                     String eventDestination, String notificationDestination) {
        this(repository, source, eventDestination, notificationDestination, "direct:integration-errors");
    }

    public DomainEventPublisherRoute(DomainEventRepository repository, String source,
                                     String eventDestination, String notificationDestination,
                                     String errorDestination) {
        this.repository = repository;
        this.source = source;
        this.eventDestination = eventDestination;
        this.notificationDestination = notificationDestination;
        this.errorDestination = errorDestination;
    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(errorDestination)
            .maximumRedeliveries(3)
            .redeliveryDelay(100)
            .onPrepareFailure(new IntegrationErrorProcessor()));

        from(source)
            .routeId("pedido-domain-event-outbox")
            .setHeader("X-Error-Origin", constant("pedido-outbox"))
            .process(exchange -> exchange.getMessage().setBody(repository.findUnpublished(100)))
            .split(body())
                .setHeader("X-Outbox-Event-Id", simple("${body.id}"))
                .setHeader("X-Correlation-Message-Id", simple("${body.messageId}"))
                .setHeader("X-Correlation-Pedido-Id", simple("${body.pedidoId}"))
                .setHeader("X-Correlation-Device-Id", simple("${body.deviceId}"))
                .setHeader("JMSType", simple("${body.eventType}"))
                .marshal().json()
                .to(eventDestination)
                .to(notificationDestination)
                .process(exchange -> repository.markPublished(
                    exchange.getMessage().getHeader("X-Outbox-Event-Id", Long.class)))
            .end();
    }
}
