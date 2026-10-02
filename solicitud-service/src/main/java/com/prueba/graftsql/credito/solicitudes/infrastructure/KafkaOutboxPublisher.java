package com.prueba.graftsql.credito.solicitudes.infrastructure;

import com.prueba.graftsql.credito.solicitudes.domain.OutboxEvent;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaOutboxPublisher {

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public KafkaOutboxPublisher(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        for (OutboxEvent event : outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(event.getTopic(), event.getEventKey(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception ignored) {
                // Se mantiene en la outbox para reintentar en el siguiente ciclo.
                return;
            }
        }
    }
}
