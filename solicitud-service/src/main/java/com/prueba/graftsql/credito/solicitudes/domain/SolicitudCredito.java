package com.prueba.graftsql.credito.solicitudes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitudes")
public class SolicitudCredito {

    @Id
    private UUID id;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;
    @Column(nullable = false)
    private int plazoMeses;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;
    @Column(nullable = false)
    private Instant creadaEn;

    protected SolicitudCredito() {
    }

    public SolicitudCredito(BigDecimal monto, int plazoMeses) {
        this.id = UUID.randomUUID();
        this.monto = monto;
        this.plazoMeses = plazoMeses;
        this.estado = EstadoSolicitud.RECIBIDA;
        this.creadaEn = Instant.now();
    }

    public UUID getId() { return id; }
    public BigDecimal getMonto() { return monto; }
    public int getPlazoMeses() { return plazoMeses; }
    public EstadoSolicitud getEstado() { return estado; }
    public Instant getCreadaEn() { return creadaEn; }
}
