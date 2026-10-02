package com.prueba.graftsql.credito.evaluaciones.infrastructure;

import com.prueba.graftsql.credito.evaluaciones.domain.OutboxEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RabbitOutboxPublisher {

    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbit;

    public RabbitOutboxPublisher(OutboxEventRepository outbox, RabbitTemplate rabbit) {
        this.outbox = outbox;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        for (OutboxEvent event : outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                rabbit.convertAndSend(event.getExchangeName(), event.getRoutingKey(), event.getPayload());
                event.markPublished();
            } catch (RuntimeException ignored) {
                // El registro queda pendiente para un reintento posterior.
                return;
            }
        }
    }
}
