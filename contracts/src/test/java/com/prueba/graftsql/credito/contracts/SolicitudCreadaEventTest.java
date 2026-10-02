package com.prueba.graftsql.credito.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudCreadaEventTest {

    @Test
    void conservaLosDatosDelContrato() {
        UUID id = UUID.randomUUID();
        SolicitudCreadaEvent event = new SolicitudCreadaEvent(id, new BigDecimal("1250.00"), 12, Instant.now());

        assertEquals(id, event.solicitudId());
        assertEquals(12, event.plazoMeses());
    }
}
