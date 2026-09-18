///usr/bin/env jbang "$0" "$@" ; exit $?

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class DeliverySmoke {
    public static void main(String... args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8081";
        String orderId = args.length > 1 ? args[1] : "PED-JBANG-SMOKE";
        String body = """
            {
              "id": "%s",
              "cliente_nombre": "Cliente JBang",
              "cliente_msisdn": "+595972000000",
              "cliente_fcm_id": "fcm_demo_jbang",
              "direccion_texto": "Asunción",
              "lat_destino": -25.2967,
              "lon_destino": -57.6359,
              "radio_llegada_m": 150,
              "repartidor_device_id": "repartidor-01"
            }
            """.formatted(orderId);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest create = HttpRequest.newBuilder(URI.create(baseUrl + "/pedidos"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> created = client.send(create, HttpResponse.BodyHandlers.ofString());
        System.out.printf("POST /pedidos -> %d%n%s%n", created.statusCode(), created.body());
        if (created.statusCode() != 202 && created.statusCode() != 409) {
            throw new IllegalStateException("Unexpected create response: " + created.statusCode());
        }

        HttpRequest tracking = HttpRequest.newBuilder(
            URI.create(baseUrl + "/pedidos/" + orderId + "/tracking")).GET().build();
        HttpResponse<String> tracked = client.send(tracking, HttpResponse.BodyHandlers.ofString());
        System.out.printf("GET /pedidos/{id}/tracking -> %d%n%s%n", tracked.statusCode(), tracked.body());
        if (tracked.statusCode() != 200) {
            throw new IllegalStateException("Unexpected tracking response: " + tracked.statusCode());
        }
    }
}
