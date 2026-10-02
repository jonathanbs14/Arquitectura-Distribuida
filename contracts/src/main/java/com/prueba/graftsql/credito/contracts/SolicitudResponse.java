package com.prueba.graftsql.credito.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolicitudResponse(
        UUID id,
        BigDecimal monto,
        int plazoMeses,
        String estado,
        Instant creadaEn) {
}
