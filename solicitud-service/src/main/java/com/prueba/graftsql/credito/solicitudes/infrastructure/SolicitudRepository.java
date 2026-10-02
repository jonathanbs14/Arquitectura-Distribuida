package com.prueba.graftsql.credito.solicitudes.infrastructure;

import com.prueba.graftsql.credito.solicitudes.domain.SolicitudCredito;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<SolicitudCredito, UUID> {
}
