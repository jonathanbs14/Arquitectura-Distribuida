package com.prueba.graftsql.credito.solicitudes.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prueba.graftsql.credito.contracts.CrearSolicitudRequest;
import com.prueba.graftsql.credito.contracts.SolicitudCreadaEvent;
import com.prueba.graftsql.credito.contracts.SolicitudResponse;
import com.prueba.graftsql.credito.solicitudes.domain.OutboxEvent;
import com.prueba.graftsql.credito.solicitudes.domain.SolicitudCredito;
import com.prueba.graftsql.credito.solicitudes.infrastructure.OutboxEventRepository;
import com.prueba.graftsql.credito.solicitudes.infrastructure.SolicitudRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitudApplicationService {

    public static final String SOLICITUD_CREADA_TOPIC = "credito.solicitud.creada.v1";
    private final SolicitudRepository solicitudes;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public SolicitudApplicationService(SolicitudRepository solicitudes, OutboxEventRepository outbox,
                                       ObjectMapper objectMapper) {
        this.solicitudes = solicitudes;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest request) {
        validar(request);
        SolicitudCredito solicitud = solicitudes.save(new SolicitudCredito(request.monto(), request.plazoMeses()));
        SolicitudCreadaEvent event = new SolicitudCreadaEvent(
                solicitud.getId(), solicitud.getMonto(), solicitud.getPlazoMeses(), Instant.now());
        outbox.save(new OutboxEvent(SOLICITUD_CREADA_TOPIC, solicitud.getId().toString(), serializar(event)));
        return toResponse(solicitud);
    }

    @Transactional(readOnly = true)
    public SolicitudResponse buscar(UUID id) {
        return solicitudes.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }

    private void validar(CrearSolicitudRequest request) {
        if (request == null || request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0
                || request.plazoMeses() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "monto y plazoMeses deben ser valores positivos");
        }
    }

    private String serializar(SolicitudCreadaEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de solicitud", exception);
        }
    }

    private SolicitudResponse toResponse(SolicitudCredito solicitud) {
        return new SolicitudResponse(solicitud.getId(), solicitud.getMonto(), solicitud.getPlazoMeses(),
                solicitud.getEstado().name(), solicitud.getCreadaEn());
    }
}
