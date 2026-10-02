package com.prueba.graftsql.credito.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/solicitudes")
    @PostMapping("/solicitudes")
    public ResponseEntity<FallbackResponse> solicitudesFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Solicitud service temporarily unavailable",
                        "SERVICE_UNAVAILABLE",
                        System.currentTimeMillis()));
    }

    @GetMapping("/evaluaciones")
    @PostMapping("/evaluaciones")
    public ResponseEntity<FallbackResponse> evaluacionesFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Evaluaciones service temporarily unavailable",
                        "SERVICE_UNAVAILABLE",
                        System.currentTimeMillis()));
    }

    @GetMapping("/auth")
    @PostMapping("/auth")
    public ResponseEntity<FallbackResponse> authFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Auth service temporarily unavailable",
                        "SERVICE_UNAVAILABLE",
                        System.currentTimeMillis()));
    }

    record FallbackResponse(String message, String code, long timestamp) {
    }
}
