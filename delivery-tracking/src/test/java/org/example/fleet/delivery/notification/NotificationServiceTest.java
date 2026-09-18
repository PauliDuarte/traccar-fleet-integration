package org.example.fleet.delivery.notification;

import org.example.fleet.delivery.events.PedidoDomainEvent;
import org.example.fleet.delivery.model.Pedido;
import org.example.fleet.delivery.model.PedidoEstado;
import org.example.fleet.delivery.repository.DomainEventRepository;
import org.example.fleet.delivery.repository.NotificationRepository;
import org.example.fleet.delivery.repository.PedidoRepository;
import org.example.fleet.delivery.repository.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {
    private DataSource dataSource;
    private PedidoRepository pedidos;
    private DomainEventRepository events;
    private FakePushGateway gateway;
    private NotificationService service;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = RepositoryTestSupport.createDatabase("notifications-" + System.nanoTime());
        pedidos = new PedidoRepository(dataSource);
        events = new DomainEventRepository(dataSource);
        gateway = new FakePushGateway();
        service = new NotificationService(new NotificationRepository(dataSource), gateway);
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        pedidos.insert(new Pedido("PED-PUSH", "Juan", "+595972222222", "fcm-demo", null,
            -25.2967, -57.6359, 150, "repartidor-01", PedidoEstado.RECIBIDO, now, now));
    }

    @Test
    void sendsFirstNotificationAndMarksEvent() throws Exception {
        PedidoDomainEvent event = transition(PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, 500);
        assertEquals(NotificationResult.SENT, service.notify(event));
        assertEquals(1, gateway.sent.size());
        assertTrue(isNotified(event.messageId()));
    }

    @Test
    void retryDoesNotSendDuplicate() throws Exception {
        PedidoDomainEvent event = transition(PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, 500);
        assertEquals(NotificationResult.SENT, service.notify(event));
        assertEquals(NotificationResult.ALREADY_SENT, service.notify(event));
        assertEquals(1, gateway.sent.size());
    }

    @Test
    void failedNotificationIsNotMarked() throws Exception {
        PedidoDomainEvent event = transition(PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, 500);
        gateway.results.add(false);
        assertEquals(NotificationResult.FAILED, service.notify(event));
        assertFalse(isNotified(event.messageId()));
    }

    @Test
    void recoversAfterFailure() throws Exception {
        PedidoDomainEvent event = transition(PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, 500);
        gateway.results.add(false);
        gateway.results.add(true);
        assertEquals(NotificationResult.FAILED, service.notify(event));
        assertEquals(NotificationResult.SENT, service.notify(event));
        assertTrue(isNotified(event.messageId()));
        assertEquals(1, gateway.sent.size());
    }

    @Test
    void eachMilestoneIsIndependent() throws Exception {
        var started = transition(PedidoEstado.RECIBIDO, PedidoEstado.EN_CAMINO, 500);
        var near = transition(PedidoEstado.EN_CAMINO, PedidoEstado.CERCA, 100);
        var delivered = transition(PedidoEstado.CERCA, PedidoEstado.ENTREGADO, 20);
        assertEquals(NotificationResult.SENT, service.notify(started));
        assertEquals(NotificationResult.SENT, service.notify(near));
        assertEquals(NotificationResult.SENT, service.notify(delivered));
        assertEquals(3, gateway.sent.size());
    }

    private PedidoDomainEvent transition(PedidoEstado from, PedidoEstado to, double distance) throws Exception {
        assertTrue(pedidos.transitionWithEvent("PED-PUSH", "repartidor-01", from, to,
            distance, "{}", Instant.now()));
        return events.findUnpublished(20).stream()
            .filter(event -> event.estadoNuevo().equals(to.name()))
            .findFirst().orElseThrow();
    }

    private boolean isNotified(String messageId) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT notificado FROM pedido_eventos WHERE message_id = ?")) {
            statement.setString(1, messageId);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static final class FakePushGateway implements PushGateway {
        private final ArrayDeque<Boolean> results = new ArrayDeque<>();
        private final List<PushNotification> sent = new ArrayList<>();

        @Override
        public boolean send(PushNotification notification) {
            boolean success = results.isEmpty() || results.removeFirst();
            if (success) {
                sent.add(notification);
            }
            return success;
        }
    }
}
