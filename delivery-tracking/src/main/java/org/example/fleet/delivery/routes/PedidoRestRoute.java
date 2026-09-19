package org.example.fleet.delivery.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.example.fleet.delivery.DeliveryRuntime;
import org.example.fleet.delivery.api.ApiError;
import org.example.fleet.delivery.api.ApiException;
import org.example.fleet.delivery.api.PedidoRequest;
import org.example.fleet.delivery.service.PedidoService;

public class PedidoRestRoute extends RouteBuilder {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final PedidoService service;
    private final boolean exposeHttp;

    public PedidoRestRoute() {
        this(DeliveryRuntime.pedidoService(), true);
    }

    public PedidoRestRoute(PedidoService service) {
        this(service, true);
    }

    public PedidoRestRoute(PedidoService service, boolean exposeHttp) {
        this.service = service;
        this.exposeHttp = exposeHttp;
    }

    @Override
    public void configure() {
        onException(ApiException.class)
            .handled(true)
            .process(exchange -> {
                ApiException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ApiException.class);
                exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.status());
                ApiError error = new ApiError(exception.code(), exception.getMessage());
                if (exposeHttp) {
                    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    exchange.getMessage().setBody(JSON.writeValueAsString(error));
                } else {
                    exchange.getMessage().setBody(error);
                }
            });

        if (exposeHttp) {
            restConfiguration()
                .component("platform-http")
                .host("0.0.0.0")
                .port(8081)
                .bindingMode(RestBindingMode.json);

            rest("/pedidos")
                .post()
                    .consumes("application/json")
                    .produces("application/json")
                    .type(PedidoRequest.class)
                    .to("direct:create-order")
                .get("/{id}/tracking")
                    .produces("application/json")
                    .to("direct:get-tracking");
        }

        from("direct:create-order")
            .routeId("create-order")
            .process(exchange -> {
                var response = service.create(exchange.getMessage().getBody(PedidoRequest.class));
                exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
                exchange.getMessage().setBody(response);
            });

        from("direct:get-tracking")
            .routeId("get-order-tracking")
            .process(exchange -> exchange.getMessage().setBody(
                service.tracking(exchange.getMessage().getHeader("id", String.class))));
    }
}
