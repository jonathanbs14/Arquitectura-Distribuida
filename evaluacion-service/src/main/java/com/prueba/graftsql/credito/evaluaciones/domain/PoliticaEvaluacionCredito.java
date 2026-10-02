package com.prueba.graftsql.credito.evaluaciones.domain;

import com.prueba.graftsql.credito.contracts.SolicitudCreadaEvent;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PoliticaEvaluacionCredito {

    public Resultado evaluar(SolicitudCreadaEvent solicitud) {
        boolean aprobado = solicitud.monto().compareTo(new BigDecimal("50000")) <= 0
                && solicitud.plazoMeses() <= 60;
        return aprobado
                ? new Resultado(true, "Cumple criterios básicos")
                : new Resultado(false, "No cumple criterios básicos");
    }

    public record Resultado(boolean aprobado, String motivo) {
    }
}
