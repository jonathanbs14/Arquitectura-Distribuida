package com.prueba.graftsql.credito.evaluaciones.infrastructure;

import com.prueba.graftsql.credito.evaluaciones.domain.EvaluacionCredito;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionRepository extends JpaRepository<EvaluacionCredito, UUID> {
    Optional<EvaluacionCredito> findBySolicitudId(UUID solicitudId);
}
