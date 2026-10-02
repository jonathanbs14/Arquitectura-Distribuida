package com.prueba.graftsql.credito.evaluaciones.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prueba.graftsql.credito.contracts.SolicitudCreadaEvent;
import com.prueba.graftsql.credito.evaluaciones.application.EvaluacionApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SolicitudCreadaKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudCreadaKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final EvaluacionApplicationService evaluaciones;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.solicitud-creada-dlq-topic:credito.solicitud.creada.v1-dlq}")
    private String dlqTopic;

    @Value("${app.kafka.max-retry-attempts:3}")
    private int maxRetryAttempts;

    public SolicitudCreadaKafkaConsumer(ObjectMapper objectMapper,
                                       EvaluacionApplicationService evaluaciones,
                                       KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.evaluaciones = evaluaciones;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${app.kafka.solicitud-creada-topic:credito.solicitud.creada.v1}",
                   groupId = "${spring.kafka.consumer.group-id:evaluacion-service-v1}")
    public void consumir(String payload) {
        try {
            logger.info("Consumiendo evento SolicitudCreada con payload: {}", payload);
            
            SolicitudCreadaEvent evento = objectMapper.readValue(payload, SolicitudCreadaEvent.class);
            evaluaciones.evaluar(evento);
            
            logger.info("Evento procesado exitosamente para solicitud: {}", evento.solicitudId());
        } catch (JsonProcessingException exception) {
            logger.error("Error al deserializar evento SolicitudCreada. Payload: {}", payload, exception);
            handleDlq(payload, "JSON_PARSE_ERROR", exception.getMessage());
            throw new IllegalArgumentException("Evento SolicitudCreada inválido", exception);
        } catch (Exception exception) {
            logger.error("Error al procesar evento SolicitudCreada", exception);
            handleDlq(payload, "PROCESSING_ERROR", exception.getMessage());
            throw new RuntimeException("Error procesando evento SolicitudCreada", exception);
        }
    }

    private void handleDlq(String payload, String errorCode, String errorMessage) {
        try {
            String dlqMessage = objectMapper.writeValueAsString(new DlqEvent(
                    payload,
                    errorCode,
                    errorMessage,
                    System.currentTimeMillis()
            ));
            kafkaTemplate.send(dlqTopic, dlqMessage);
            logger.info("Mensaje enviado a DLQ: {}", dlqTopic);
        } catch (Exception dlqException) {
            logger.error("Error al enviar mensaje a DLQ", dlqException);
        }
    }

    record DlqEvent(String originalPayload, String errorCode, String errorMessage, long timestamp) {
    }
}

