package org.example.fleet.delivery.notification;

@FunctionalInterface
public interface PushGateway {
    boolean send(PushNotification notification) throws Exception;
}
