package com.prueba.graftsql.credito.evaluaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String exchangeName;
    @Column(nullable = false)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String exchangeName, String routingKey, String payload) {
        this.id = UUID.randomUUID();
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public String getExchangeName() { return exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public void markPublished() { this.publishedAt = Instant.now(); }
}
