package com.prueba.graftsql.credito.evaluaciones.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prueba.graftsql.credito.contracts.SolicitudCreadaEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PoliticaEvaluacionCreditoTest {

    private final PoliticaEvaluacionCredito politica = new PoliticaEvaluacionCredito();

    @Test
    void apruebaSolicitudDentroDeLosLimites() {
        var solicitud = new SolicitudCreadaEvent(UUID.randomUUID(), new BigDecimal("50000"), 60, Instant.now());
        assertTrue(politica.evaluar(solicitud).aprobado());
    }

    @Test
    void rechazaSolicitudFueraDeLosLimites() {
        var solicitud = new SolicitudCreadaEvent(UUID.randomUUID(), new BigDecimal("50001"), 60, Instant.now());
        assertFalse(politica.evaluar(solicitud).aprobado());
    }
}
