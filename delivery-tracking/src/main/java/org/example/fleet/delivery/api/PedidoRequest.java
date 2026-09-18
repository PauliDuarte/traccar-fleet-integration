package org.example.fleet.delivery.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PedidoRequest(
    String id,
    @JsonProperty("cliente_nombre") String clienteNombre,
    @JsonProperty("cliente_msisdn") String clienteMsisdn,
    @JsonProperty("cliente_fcm_id") String clienteFcmId,
    @JsonProperty("direccion_texto") String direccionTexto,
    @JsonProperty("lat_destino") Double latDestino,
    @JsonProperty("lon_destino") Double lonDestino,
    @JsonProperty("radio_llegada_m") Integer radioLlegadaM,
    @JsonProperty("repartidor_device_id") String repartidorDeviceId
) {
}
