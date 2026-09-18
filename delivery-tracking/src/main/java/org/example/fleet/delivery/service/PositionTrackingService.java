package org.example.fleet.delivery.service;

import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.model.UltimaPosicion;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.model.VehiclePosition;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class PositionTrackingService {
    private final PedidoRepository pedidos;
    private final PosicionRepository posiciones;
    private final EventoRepository eventos;

    public PositionTrackingService(PedidoRepository pedidos, PosicionRepository posiciones,
                                   EventoRepository eventos) {
        this.pedidos = pedidos;
        this.posiciones = posiciones;
        this.eventos = eventos;
    }

    public PositionResult process(VehiclePosition position) throws SQLException {
        if (!isValid(position)) {
            return PositionResult.INVALID;
        }
        Pedido pedido = pedidos.findActiveByDeviceId(position.deviceId()).orElse(null);
        if (pedido == null) {
            return PositionResult.NO_ACTIVE_ORDER;
        }

        Instant gpsTimestamp = Instant.parse(position.timestamp());
        var previousPosition = posiciones.findByPedidoId(pedido.id());
        if (previousPosition.isPresent() && !gpsTimestamp.isAfter(previousPosition.get().timestamp())) {
            return PositionResult.STALE;
        }
        double distance = Haversine.distanceMeters(
            position.latitude(), position.longitude(), pedido.latDestino(), pedido.lonDestino());
        posiciones.upsert(new UltimaPosicion(pedido.id(), position.deviceId(), position.messageId(),
            position.latitude(), position.longitude(), position.speedKmh(), distance, gpsTimestamp));

        String detail = detail(position, distance);
        if (pedido.estado() == PedidoEstado.RECIBIDO
            && pedidos.transitionWithEvent(pedido.id(), PedidoEstado.RECIBIDO,
                PedidoEstado.EN_CAMINO, detail, gpsTimestamp)) {
            return PositionResult.STARTED;
        }
        if (pedido.estado() == PedidoEstado.EN_CAMINO && distance <= pedido.radioLlegadaM()
            && pedidos.transitionWithEvent(pedido.id(), PedidoEstado.EN_CAMINO,
                PedidoEstado.CERCA, detail, gpsTimestamp)) {
            return PositionResult.NEAR;
        }
        if (pedido.estado() == PedidoEstado.CERCA && distance <= pedido.radioLlegadaM()
            && position.speedKmh() <= 3 && previousPosition.isPresent()
            && pedidos.transitionWithEvent(pedido.id(), PedidoEstado.CERCA,
                PedidoEstado.ENTREGADO, detail, gpsTimestamp)) {
            return PositionResult.DELIVERED;
        }
        return PositionResult.UPDATED;
    }

    private static boolean isValid(VehiclePosition position) {
        if (position == null || !position.valid() || blank(position.messageId()) || blank(position.deviceId())
            || blank(position.timestamp()) || !Double.isFinite(position.latitude())
            || !Double.isFinite(position.longitude()) || position.latitude() < -90 || position.latitude() > 90
            || position.longitude() < -180 || position.longitude() > 180) {
            return false;
        }
        try {
            Instant.parse(position.timestamp());
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String detail(VehiclePosition position, double distance) {
        return "{\"message_id\":\"" + escape(position.messageId())
            + "\",\"device_id\":\"" + escape(position.deviceId())
            + "\",\"distancia_destino_m\":" + distance + "}";
    }
}
