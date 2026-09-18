package org.example.fleet.delivery;

import org.example.fleet.delivery.db.DataSourceFactory;
import org.example.fleet.delivery.repository.EventoRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.PosicionRepository;
import org.example.fleet.delivery.repository.RepartidorRepository;
import org.example.fleet.delivery.service.PedidoService;
import org.example.fleet.delivery.service.PositionTrackingService;

import javax.sql.DataSource;
import java.time.Clock;

public final class DeliveryRuntime {
    private static final DataSource DATA_SOURCE = DataSourceFactory.fromEnvironment();
    private static final PedidoRepository PEDIDOS = new PedidoRepository(DATA_SOURCE);
    private static final RepartidorRepository REPARTIDORES = new RepartidorRepository(DATA_SOURCE);
    private static final PosicionRepository POSICIONES = new PosicionRepository(DATA_SOURCE);
    private static final EventoRepository EVENTOS = new EventoRepository(DATA_SOURCE);
    private static final PedidoService PEDIDO_SERVICE = new PedidoService(
        PEDIDOS, REPARTIDORES, POSICIONES, EVENTOS, Clock.systemUTC());
    private static final PositionTrackingService POSITION_SERVICE = new PositionTrackingService(PEDIDOS, POSICIONES);

    private DeliveryRuntime() {
    }

    public static PedidoService pedidoService() {
        return PEDIDO_SERVICE;
    }

    public static PositionTrackingService positionService() {
        return POSITION_SERVICE;
    }

    public static DataSource dataSource() {
        return DATA_SOURCE;
    }
}
