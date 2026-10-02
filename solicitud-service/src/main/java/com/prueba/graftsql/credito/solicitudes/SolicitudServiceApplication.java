package com.prueba.graftsql.credito.solicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SolicitudServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolicitudServiceApplication.class, args);
    }
}
