package com.prueba.graftsql.credito.evaluaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EvaluacionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluacionServiceApplication.class, args);
    }
}
