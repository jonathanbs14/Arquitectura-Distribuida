package com.prueba.graftsql.credito.evaluaciones.web;

import com.prueba.graftsql.credito.contracts.EvaluacionResponse;
import com.prueba.graftsql.credito.evaluaciones.application.EvaluacionApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evaluaciones")
public class EvaluacionController {

    private final EvaluacionApplicationService evaluaciones;

    public EvaluacionController(EvaluacionApplicationService evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    @GetMapping("/{solicitudId}")
    public EvaluacionResponse buscar(@PathVariable UUID solicitudId) {
        return evaluaciones.buscar(solicitudId);
    }
}
