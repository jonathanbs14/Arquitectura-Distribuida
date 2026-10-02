package com.prueba.graftsql.credito.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato versionado que se publica en Kafka, tópico credito.solicitud.creada.v1. */
public record SolicitudCreadaEvent(
        UUID solicitudId,
        BigDecimal monto,
        int plazoMeses,
        Instant occurredAt) {
}
