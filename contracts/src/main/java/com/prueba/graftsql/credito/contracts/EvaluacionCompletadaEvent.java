package com.prueba.graftsql.credito.contracts;

import java.time.Instant;
import java.util.UUID;

/** Contrato publicado por RabbitMQ en el exchange credito.evaluaciones.v1. */
public record EvaluacionCompletadaEvent(
        UUID solicitudId,
        boolean aprobado,
        String motivo,
        Instant occurredAt) {
}
