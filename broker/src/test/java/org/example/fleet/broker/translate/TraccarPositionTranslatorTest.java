package org.example.fleet.broker.translate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraccarPositionTranslatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TraccarPositionTranslator translator = new TraccarPositionTranslator();

    @Test
    void usesDeviceUniqueIdFromRealForwardEnvelope() throws Exception {
        var envelope = mapper.readTree("""
            {"position":{"deviceId":1,"latitude":-25.31,"longitude":-57.65,"speed":10,
            "course":0,"valid":true,"fixTime":"2026-09-19T00:14:53Z","attributes":{}},
            "device":{"id":1,"uniqueId":"repartidor-01"}}
            """);

        assertEquals("repartidor-01", translator.translate(envelope).deviceId());
    }

    @Test
    void fallsBackToInternalDeviceIdForLegacyPayload() throws Exception {
        var envelope = mapper.readTree("""
            {"position":{"deviceId":7,"latitude":-25.31,"longitude":-57.65,"speed":0,
            "course":0,"valid":true,"fixTime":"2026-09-19T00:14:53Z","attributes":{}}}
            """);

        assertEquals("7", translator.translate(envelope).deviceId());
    }
}
