# Implementación de Circuit Breaker + Retry y Kafka DLQ

## Cambios Realizados

### 1. **API Gateway - Circuit Breaker + Retry con Resilience4j**

#### Archivos Modificados:
- `api-gateway/build.gradle` - Agregada dependencia de Resilience4j
- `api-gateway/src/main/resources/application.yml` - Configuración de circuit breaker y retry
- `api-gateway/src/main/java/.../FallbackController.java` - Nuevo controlador para fallbacks

#### Características:
- **Circuit Breaker**: Protege el API Gateway de servicios no disponibles
  - Estados: CLOSED (normal) → OPEN (fallando) → HALF_OPEN (recuperando)
  - Umbral de falla: 50% de las peticiones
  - Tiempo en OPEN: 5 segundos
  
- **Retry**: Reintentos automáticos en caso de fallo
  - Máximo de reintentos: 3
  - Backoff exponencial: 100ms inicial, 1000ms máximo, 1.5x multiplicador
  - Métodos: GET y POST
  
- **Fallback**: Respuesta amigable (503 Service Unavailable) cuando el servicio no está disponible

#### Monitoreo:
```bash
# Health check del circuit breaker
curl http://localhost:8080/actuator/health

# Detalles del circuit breaker
curl http://localhost:8080/actuator/circuitbreaker
```

---

### 2. **Evaluación Service - Kafka Dead Letter Queue (DLQ)**

#### Archivos Modificados:
- `evaluacion-service/build.gradle` - Agregadas dependencias para Kafka
- `evaluacion-service/src/main/resources/application.yml` - Configuración de Kafka y DLQ
- `evaluacion-service/src/main/java/.../SolicitudCreadaKafkaConsumer.java` - Manejo de errores y DLQ
- `evaluacion-service/src/main/java/.../SolicitudCreadaDlqConsumer.java` - Nuevo consumer para DLQ
- `evaluacion-service/src/main/java/.../KafkaConsumerConfig.java` - Configuración de Kafka

#### Características:
- **DLQ (Dead Letter Queue)**: Tópico `credito.solicitud.creada.v1-dlq`
  - Eventos que fallan después de reintentos se envían aquí
  - Permite análisis y recuperación manual
  
- **Consumer Resiliente**:
  - Logging detallado de errores
  - Envío automático a DLQ en caso de fallo
  - Sin pérdida de mensajes
  
- **Configuración de Kafka**:
  - Consumer group: `evaluacion-service-v1`
  - Auto-commit deshabilitado (manual ack)
  - Concurrencia: 3 listeners
  - Max poll records: 10

#### Tópicos Kafka:
```
credito.solicitud.creada.v1      → Tópico principal
credito.solicitud.creada.v1-dlq  → Dead Letter Queue
```

---

## Flujo de Funcionamiento

### Circuit Breaker en API Gateway:
```
Cliente → API Gateway
              ↓
        Circuit Breaker
              ↓
        ¿Servicio disponible?
              ↙              ↘
            SÍ              NO
            ↓               ↓
        Servicio    Fallback (503)
        Normal      + Log error
```

### Kafka Consumer con DLQ:
```
Mensaje en Kafka
       ↓
SolicitudCreadaConsumer
       ↓
¿Procesamiento exitoso?
      ↙              ↘
    SÍ              NO
    ↓               ↓
  ACK        ¿Reintentos disponibles?
            ↙                    ↘
          SÍ                    NO
          ↓                     ↓
      Reintento        DLQ Consumer
      (backoff)        + Log error
```

---

## Testing

### 1. Verificar Circuit Breaker
```bash
# Verificar que el circuit breaker está activo
curl -s http://localhost:8080/actuator/health | jq .

# Detener un servicio y ver que el circuit breaker abre
docker stop <service-name>

# Verificar fallback en respuesta
curl -s http://localhost:8080/solicitudes | jq .

# Reiniciar servicio
docker start <service-name>
```

### 2. Verificar Kafka DLQ
```bash
# Enviar un mensaje inválido a Kafka
kafka-console-producer --broker-list localhost:9092 --topic credito.solicitud.creada.v1
> {"invalid":"json"

# Verificar que se envía a DLQ
kafka-console-consumer --bootstrap-server localhost:9092 --topic credito.solicitud.creada.v1-dlq --from-beginning
```

---

## Beneficios

✅ **Resilencia**: El sistema se recupera automáticamente de fallos transitorios
✅ **Observabilidad**: Logging detallado de errores y eventos fallidos
✅ **Confiabilidad**: Mensajes no se pierden, van a DLQ para análisis
✅ **Performance**: Backoff exponencial evita sobrecargar servicios
✅ **Mantenibilidad**: Código limpio y fácil de debuggear

---

## Configuración en Despliegue

Para ambiente de producción, ajustar en `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      solicitudCircuitBreaker:
        failureRateThreshold: 30      # Más sensible
        waitDurationInOpenState: 30s  # Espera más antes de reintentar
        
  timelimiter:
    instances:
      solicitudCircuitBreaker:
        timeoutDuration: 5s           # Timeout más largo

app:
  kafka:
    max-retry-attempts: 5            # Más reintentos
    initial-interval-ms: 2000        # Espera inicial mayor
    max-interval-ms: 30000           # Espera máxima mayor
```
