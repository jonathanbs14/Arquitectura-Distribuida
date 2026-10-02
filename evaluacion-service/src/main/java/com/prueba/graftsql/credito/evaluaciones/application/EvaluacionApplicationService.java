package com.prueba.graftsql.credito.evaluaciones.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prueba.graftsql.credito.contracts.EvaluacionCompletadaEvent;
import com.prueba.graftsql.credito.contracts.EvaluacionResponse;
import com.prueba.graftsql.credito.contracts.SolicitudCreadaEvent;
import com.prueba.graftsql.credito.evaluaciones.domain.EvaluacionCredito;
import com.prueba.graftsql.credito.evaluaciones.domain.OutboxEvent;
import com.prueba.graftsql.credito.evaluaciones.domain.PoliticaEvaluacionCredito;
import com.prueba.graftsql.credito.evaluaciones.infrastructure.EvaluacionRepository;
import com.prueba.graftsql.credito.evaluaciones.infrastructure.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvaluacionApplicationService {

    public static final String EVALUACIONES_EXCHANGE = "credito.evaluaciones.v1";
    public static final String EVALUACION_COMPLETADA_KEY = "evaluacion.completada";
    private final EvaluacionRepository evaluaciones;
    private final OutboxEventRepository outbox;
    private final PoliticaEvaluacionCredito politica;
    private final ObjectMapper objectMapper;

    public EvaluacionApplicationService(EvaluacionRepository evaluaciones, OutboxEventRepository outbox,
                                        PoliticaEvaluacionCredito politica, ObjectMapper objectMapper) {
        this.evaluaciones = evaluaciones;
        this.outbox = outbox;
        this.politica = politica;
        this.objectMapper = objectMapper;
    }

    /** La clave única de solicitudId vuelve idempotente al consumidor ante reentregas de Kafka. */
    @Transactional
    public void evaluar(SolicitudCreadaEvent solicitud) {
        if (evaluaciones.findBySolicitudId(solicitud.solicitudId()).isPresent()) {
            return;
        }
        PoliticaEvaluacionCredito.Resultado resultado = politica.evaluar(solicitud);
        EvaluacionCredito evaluacion = evaluaciones.save(
                new EvaluacionCredito(solicitud.solicitudId(), resultado.aprobado(), resultado.motivo()));
        EvaluacionCompletadaEvent event = new EvaluacionCompletadaEvent(evaluacion.getSolicitudId(),
                evaluacion.isAprobado(), evaluacion.getMotivo(), Instant.now());
        outbox.save(new OutboxEvent(EVALUACIONES_EXCHANGE, EVALUACION_COMPLETADA_KEY, serializar(event)));
    }

    @Transactional(readOnly = true)
    public EvaluacionResponse buscar(UUID solicitudId) {
        return evaluaciones.findBySolicitudId(solicitudId).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La evaluación aún no está disponible"));
    }

    private String serializar(EvaluacionCompletadaEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de evaluación", exception);
        }
    }

    private EvaluacionResponse toResponse(EvaluacionCredito evaluacion) {
        return new EvaluacionResponse(evaluacion.getSolicitudId(), evaluacion.isAprobado(),
                evaluacion.getMotivo(), evaluacion.getEvaluadaEn());
    }
}
