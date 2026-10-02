package com.prueba.graftsql.credito.solicitudes.domain;

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
    private String topic;
    @Column(nullable = false)
    private String eventKey;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String eventKey, String payload) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public String getTopic() { return topic; }
    public String getEventKey() { return eventKey; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return publishedAt != null; }
    public void markPublished() { this.publishedAt = Instant.now(); }
}
