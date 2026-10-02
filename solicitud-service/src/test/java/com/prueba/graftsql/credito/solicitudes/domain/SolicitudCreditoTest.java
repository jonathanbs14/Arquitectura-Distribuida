package com.prueba.graftsql.credito.solicitudes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SolicitudCreditoTest {

    @Test
    void nuevaSolicitudEmpiezaRecibida() {
        SolicitudCredito solicitud = new SolicitudCredito(new BigDecimal("2000.00"), 24);

        assertNotNull(solicitud.getId());
        assertEquals(EstadoSolicitud.RECIBIDA, solicitud.getEstado());
    }
}
