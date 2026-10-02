package com.prueba.graftsql.credito.contracts;

import java.time.Instant;
import java.util.UUID;

public record EvaluacionResponse(
        UUID solicitudId,
        boolean aprobado,
        String motivo,
        Instant evaluadaEn) {
}
