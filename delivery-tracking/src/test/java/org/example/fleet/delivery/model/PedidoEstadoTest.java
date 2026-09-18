package org.example.fleet.delivery.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoEstadoTest {
    @Test
    void containsTheDefinedLifecycleStates() {
        assertEquals(5, PedidoEstado.values().length);
        assertEquals(PedidoEstado.CANCELADO, PedidoEstado.valueOf("CANCELADO"));
    }
}
