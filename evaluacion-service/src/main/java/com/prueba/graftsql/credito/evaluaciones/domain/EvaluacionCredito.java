package com.prueba.graftsql.credito.evaluaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluaciones")
public class EvaluacionCredito {

    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private UUID solicitudId;
    @Column(nullable = false)
    private boolean aprobado;
    @Column(nullable = false)
    private String motivo;
    @Column(nullable = false)
    private Instant evaluadaEn;

    protected EvaluacionCredito() {
    }

    public EvaluacionCredito(UUID solicitudId, boolean aprobado, String motivo) {
        this.id = UUID.randomUUID();
        this.solicitudId = solicitudId;
        this.aprobado = aprobado;
        this.motivo = motivo;
        this.evaluadaEn = Instant.now();
    }

    public UUID getSolicitudId() { return solicitudId; }
    public boolean isAprobado() { return aprobado; }
    public String getMotivo() { return motivo; }
    public Instant getEvaluadaEn() { return evaluadaEn; }
}
