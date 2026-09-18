package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;
import org.example.fleet.delivery.DeliveryRuntime;
import org.example.fleet.delivery.service.PositionTrackingService;
import org.example.fleet.model.VehiclePosition;

public class VehiclePositionRoute extends RouteBuilder {
    private static final String AMQP_SOURCE = "amqp:topic:vehicle.positions"
        + "?subscriptionDurable=true"
        + "&durableSubscriptionName=delivery-tracking"
        + "&clientId=delivery-tracking";

    private final PositionTrackingService service;
    private final String source;

    public VehiclePositionRoute() {
        this(DeliveryRuntime.positionService(), AMQP_SOURCE);
    }

    public VehiclePositionRoute(PositionTrackingService service, String source) {
        this.service = service;
        this.source = source;
    }

    @Override
    public void configure() {
        from(source)
            .routeId("delivery-vehicle-positions")
            .unmarshal().json(VehiclePosition.class)
            .setHeader("X-Correlation-Device-Id", simple("${body.deviceId}"))
            .setHeader("X-Correlation-Message-Id", simple("${body.messageId}"))
            .filter(body().method("valid"))
                .process(exchange -> service.process(exchange.getMessage().getBody(VehiclePosition.class)))
            .end();
    }
}
