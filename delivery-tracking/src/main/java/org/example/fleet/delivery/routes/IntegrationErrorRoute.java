package org.example.fleet.delivery.routes;

import org.apache.camel.builder.RouteBuilder;

public class IntegrationErrorRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:integration-errors")
            .routeId("integration-error-channel")
            .marshal().json()
            .to("amqp:queue:integration.errors");
    }
}
