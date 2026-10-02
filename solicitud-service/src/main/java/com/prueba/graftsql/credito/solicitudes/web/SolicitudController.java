package com.prueba.graftsql.credito.solicitudes.web;

import com.prueba.graftsql.credito.contracts.CrearSolicitudRequest;
import com.prueba.graftsql.credito.contracts.SolicitudResponse;
import com.prueba.graftsql.credito.solicitudes.application.SolicitudApplicationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudApplicationService solicitudes;

    public SolicitudController(SolicitudApplicationService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@RequestBody CrearSolicitudRequest request) {
        SolicitudResponse created = solicitudes.crear(request);
        return ResponseEntity.created(URI.create("/solicitudes/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public SolicitudResponse buscar(@PathVariable UUID id) {
        return solicitudes.buscar(id);
    }
}
