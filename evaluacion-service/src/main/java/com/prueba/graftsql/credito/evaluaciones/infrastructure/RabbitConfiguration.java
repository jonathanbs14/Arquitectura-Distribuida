package com.prueba.graftsql.credito.evaluaciones.infrastructure;

import com.prueba.graftsql.credito.evaluaciones.application.EvaluacionApplicationService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    TopicExchange evaluacionesExchange() {
        return new TopicExchange(EvaluacionApplicationService.EVALUACIONES_EXCHANGE, true, false);
    }

    @Bean
    Queue evaluacionesCompletadasQueue() {
        return new Queue("credito.evaluacion.completada.v1", true);
    }

    @Bean
    Binding evaluacionesBinding(Queue evaluacionesCompletadasQueue, TopicExchange evaluacionesExchange) {
        return BindingBuilder.bind(evaluacionesCompletadasQueue).to(evaluacionesExchange)
                .with(EvaluacionApplicationService.EVALUACION_COMPLETADA_KEY);
    }
}
