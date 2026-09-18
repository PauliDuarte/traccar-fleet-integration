package org.example.fleet.delivery.service;

import org.example.fleet.delivery.api.ApiException;
import org.example.fleet.delivery.api.PedidoCreatedResponse;
import org.example.fleet.delivery.api.PedidoRequest;
import org.example.fleet.delivery.api.TrackingPositionResponse;
import org.example.fleet.delivery.api.TrackingResponse;
import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.delivery.repository.RepartidorRepository;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;

public final class PedidoService {
    private final PedidoRepository pedidos;
    private final RepartidorRepository repartidores;
    private final PosicionRepository posiciones;
    private final EventoRepository eventos;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidos, RepartidorRepository repartidores,
                         PosicionRepository posiciones, EventoRepository eventos, Clock clock) {
        this.pedidos = pedidos;
        this.repartidores = repartidores;
        this.posiciones = posiciones;
        this.eventos = eventos;
        this.clock = clock;
    }

    public PedidoCreatedResponse create(PedidoRequest request) {
        validate(request);
        try {
            if (!repartidores.existsByDeviceId(request.repartidorDeviceId())) {
                throw new ApiException("REPARTIDOR_DESCONOCIDO", "El repartidor no existe", 400);
            }
            if (pedidos.findById(request.id()).isPresent()) {
                throw new ApiException("PEDIDO_DUPLICADO", "Ya existe un pedido con ese ID", 409);
            }
            Instant now = clock.instant();
            Pedido pedido = new Pedido(request.id(), request.clienteNombre(), request.clienteMsisdn(),
                request.clienteFcmId(), request.direccionTexto(), request.latDestino(), request.lonDestino(),
                request.radioLlegadaM(), request.repartidorDeviceId(), PedidoEstado.RECIBIDO, now, now);
            pedidos.insert(pedido);
            eventos.insertIfAbsent(pedido.id(), PedidoEstado.RECIBIDO.name(), "{}", now);
            return new PedidoCreatedResponse(pedido.id(), pedido.estado(), pedido.fechaCreacion());
        } catch (ApiException exception) {
            throw exception;
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new ApiException("PEDIDO_DUPLICADO", "Ya existe el pedido o una asignación activa", 409);
            }
            throw new ApiException("REQUEST_INVALIDO", "No fue posible registrar el pedido", 500);
        }
    }

    public TrackingResponse tracking(String pedidoId) {
        try {
            Pedido pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new ApiException("PEDIDO_NO_ENCONTRADO", "El pedido no existe", 404));
            TrackingPositionResponse position = posiciones.findByPedidoId(pedidoId)
                .map(value -> new TrackingPositionResponse(value.lat(), value.lon(), value.velocidadKmh(),
                    value.distanciaDestinoM(), value.timestamp()))
                .orElse(null);
            return new TrackingResponse(pedido.id(), pedido.estado(), position);
        } catch (ApiException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new ApiException("REQUEST_INVALIDO", "No fue posible consultar el pedido", 500);
        }
    }

    private static void validate(PedidoRequest request) {
        if (request == null || blank(request.id()) || blank(request.clienteNombre())
            || blank(request.clienteMsisdn()) || blank(request.repartidorDeviceId())
            || request.latDestino() == null || request.lonDestino() == null
            || request.radioLlegadaM() == null || request.radioLlegadaM() <= 0) {
            throw new ApiException("REQUEST_INVALIDO", "Faltan campos obligatorios o son inválidos", 400);
        }
        if (request.latDestino() < -90 || request.latDestino() > 90
            || request.lonDestino() < -180 || request.lonDestino() > 180) {
            throw new ApiException("COORDENADAS_INVALIDAS", "Las coordenadas están fuera de rango", 400);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
