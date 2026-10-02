package com.prueba.graftsql.credito.evaluaciones.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SolicitudCreadaDlqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudCreadaDlqConsumer.class);

    @KafkaListener(topics = "${app.kafka.solicitud-creada-dlq-topic:credito.solicitud.creada.v1-dlq}",
                   groupId = "${spring.kafka.consumer.group-id:evaluacion-service-v1}-dlq")
    public void consumirDlq(String payload) {
        logger.error("EVENTO FALLIDO EN DLQ | Payload: {}", payload);
        
        // TODO: Aquí se podría implementar:
        // - Persistencia en base de datos para análisis posterior
        // - Alertas/notificaciones
        // - Re-procesamiento manual
    }
}
