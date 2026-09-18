package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;
import org.example.fleet.delivery.DeliveryRuntime;
import org.example.fleet.delivery.error.IntegrationErrorProcessor;
import org.example.fleet.delivery.service.PositionResult;
import org.example.fleet.delivery.service.PositionTrackingService;
import org.example.fleet.model.VehiclePosition;

public class VehiclePositionRoute extends RouteBuilder {
    private static final String AMQP_SOURCE = "amqp:topic:vehicle.positions"
        + "?subscriptionDurable=true"
        + "&durableSubscriptionName=delivery-tracking"
        + "&clientId=delivery-tracking";

    private final PositionTrackingService service;
    private final String source;
    private final String errorDestination;

    public VehiclePositionRoute() {
        this(DeliveryRuntime.positionService(), AMQP_SOURCE, "direct:integration-errors");
    }

    public VehiclePositionRoute(PositionTrackingService service, String source) {
        this(service, source, "direct:integration-errors");
    }

    public VehiclePositionRoute(PositionTrackingService service, String source, String errorDestination) {
        this.service = service;
        this.source = source;
        this.errorDestination = errorDestination;
    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(errorDestination)
            .maximumRedeliveries(3)
            .redeliveryDelay(100)
            .onPrepareFailure(new IntegrationErrorProcessor()));

        from(source)
            .routeId("delivery-vehicle-positions")
            .setHeader("X-Error-Origin", constant("vehicle.positions"))
            .unmarshal().json(VehiclePosition.class)
            .setHeader("X-Correlation-Device-Id", simple("${body.deviceId}"))
            .setHeader("X-Correlation-Message-Id", simple("${body.messageId}"))
            .filter(body().method("valid"))
                .process(exchange -> {
                    PositionResult result = service.process(exchange.getMessage().getBody(VehiclePosition.class));
                    exchange.getMessage().setHeader("X-Position-Result", result.name());
                })
            .end();
    }
}
