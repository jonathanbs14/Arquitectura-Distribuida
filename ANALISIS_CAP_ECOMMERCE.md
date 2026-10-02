# Análisis CAP: Plataforma E-commerce Black Friday

## 🎯 Caso: Platform E-commerce en Black Friday

### 📋 Análisis por Componente

---

## 1️⃣ CARRITO DE COMPRAS

### Pregunta: ¿Es aceptable que dos dispositivos vean carritos distintos por unos segundos?

**Respuesta: SÍ, es aceptable**

| Aspecto | Decisión |
|---------|----------|
| **Clasificación CAP** | **AP** (Availability + Partition Tolerance) |
| **Consistencia** | **Eventual (débil)** |
| **Nivel SLA** | 99.99% disponibilidad, 2-5s de inconsistencia máximo |

### Justificación:
- ✅ Si el carrito está **down**: Cliente puede navegar, pero no comprar (menos malo que perder la venta)
- ✅ Inconsistencia temporal: "Me agrega algo y tarda en verse en otro dispositivo" es **aceptable**
- ✅ Afecta UX pero NO dinero

### Patrón de Resiliencia:
```
┌─────────────────────────────┐
│  CLIENTE                    │
│ (Dispositivo A)             │
└──────────┬──────────────────┘
           │ Agrega producto
           ▼
┌─────────────────────────────┐
│  CARRITO SERVICE (Write)    │
│  - Escritura inmediata      │
│  - Responde OK              │
│  - Publica evento           │
└──────────┬──────────────────┘
           │ Evento: CarritoActualizado
           ▼
┌─────────────────────────────┐
│  CACHE DISTRIBUIDO (Redis)  │
│  - Actualización eventual   │
│  - 2-3s de delay            │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  CLIENTE                    │
│ (Dispositivo B)             │
│ Ve el carrito actualizado   │
└─────────────────────────────┘
```

### Patrón: **CQRS + Event Sourcing**
```yaml
Write Model:
  - Carrito actualizado inmediatamente
  - BD transaccional
  - Responde al cliente: OK
  
Read Model (Cache):
  - Se actualiza de forma eventual
  - Redis/Memcached
  - Delay: 2-5 segundos
```

### Monitoreo:
```bash
# Métrica: Inconsistency lag
carrito.cache.lag_seconds = 2-5  # Aceptable
carrito.cache.lag_seconds > 10   # Alerta
```

---

## 2️⃣ INVENTARIO

### Pregunta: ¿Es aceptable vender la última unidad dos veces?

**Respuesta: NO, es inaceptable**

| Aspecto | Decisión |
|---------|----------|
| **Clasificación CAP** | **CP** (Consistency + Partition Tolerance) |
| **Consistencia** | **Fuerte (Strict)** |
| **Nivel SLA** | 99.9% consistencia garantizada, CERO oversell |

### Justificación:
- ❌ Oversell = Problema de negocio crítico
- ❌ 2 clientes para 1 producto = Pérdida, devoluciones, reputación
- ❌ "Es raro que pase" NO es aceptable

### Patrón de Resiliencia: **DISTRIBUTED LOCK + SAGA**

```
CLIENTE A                    CLIENTE B
     │                           │
     └─────────────────┬─────────┘
                       │
                    GATEWAY
                       │
            ┌──────────┴──────────┐
            │                     │
       INVENTARIO SERVICE    INVENTARIO SERVICE
       (Intenta comprar)      (Intenta comprar)
            │                     │
            └──────────┬──────────┘
                       │
            ┌──────────▼──────────┐
            │  LOCK DISTRIBUIDO   │
            │  (Redis/Zookeeper)  │
            │  Key: SKU-12345     │
            │  TTL: 5s            │
            └──────────┬──────────┘
                       │
        Solo UNO adquiere lock
                       │
        ┌──────────────┴──────────────┐
        │ (Gana lock)                 │ (No gana lock)
        │ Lee stock: 1                │ Espera o retry
        ▼                              ▼
    Decrement a 0              Intenta de nuevo
    Genera transacción      (El stock ya es 0)
    Commit                  Responde: AGOTADO
        │
    Libera lock
```

### Implementación:

```java
// Pseudocódigo
public void comprar(String sku, int cantidad) {
    // 1. Acquire distributed lock (5s timeout)
    Lock lock = distributedLockManager.acquire("SKU-" + sku, 5000);
    
    try {
        // 2. Read stock (within lock)
        int stock = inventarioRepository.getStock(sku);
        
        // 3. Check disponibilidad
        if (stock < cantidad) {
            throw new StockInsuficienteException();
        }
        
        // 4. Decrement atomically
        inventarioRepository.decrementStock(sku, cantidad);
        
        // 5. Guardar transacción
        transactionRepository.save(new Transaccion(sku, cantidad, "COMPLETED"));
        
    } finally {
        // 6. Release lock
        lock.release();
    }
}
```

### Configuración de Inventario:

```yaml
# application.yml
inventario:
  consistency-level: STRONG
  lock:
    provider: redis
    timeout-ms: 5000
    max-retries: 3
    retry-delay-ms: 100
  
  # En caso de fallo de lock
  fallback: DENY_SALE  # Siempre denegar, no permitir
```

---

## 3️⃣ PAGOS

### Pregunta: ¿Qué pasa si el gateway responde 'timeout' — reintentar o no?

**Respuesta: DEPENDE del tipo de timeout**

| Aspecto | Decisión |
|---------|----------|
| **Clasificación CAP** | **CP** (Consistency + Partition Tolerance) |
| **Consistencia** | **Fuerte + Idempotencia** |
| **Nivel SLA** | 99.99% de pagos procesados correctamente |

### Tipos de Timeout y Estrategia:

```
┌─────────────────────────────────────────────────┐
│ CLIENTE envía: $500 a PaymentGateway            │
└────────────────┬────────────────────────────────┘
                 │
         ┌───────▼────────┐
         │ Se envía pago   │
         │ a Gateway       │
         └───────┬────────┘
                 │
     ┌───────────┼───────────┬───────────┐
     │           │           │           │
  TIMEOUT      RECHAZADO   PROCESADO    ERROR
  (0-3s)       (4xx)       (200 OK)     (500)
     │           │           │           │
     ▼           ▼           ▼           ▼
  ¿QUÉ?        DENY      ACCEPT       ¿QUÉ?
```

### Matriz de Decisión:

| Respuesta Gateway | Acción | Reintentar | Razón |
|-------------------|--------|-----------|--------|
| **200 OK** | ✅ ACEPTAR | NO | Pago confirmado |
| **4xx (Bad Request)** | ❌ RECHAZAR | NO | Culpa del cliente, no reintentar |
| **402 (Insufficient Funds)** | ❌ RECHAZAR | NO | No hay dinero |
| **403 (Card Declined)** | ❌ RECHAZAR | NO | Tarjeta rechazada |
| **500 (Server Error)** | ⚠️ PENDIENTE | SÍ (3x) | Error temporal del gateway |
| **503 (Unavailable)** | ⚠️ PENDIENTE | SÍ (5x) | Gateway caído |
| **TIMEOUT (> 3s)** | ⚠️ CONSULTAR | SÍ (QUERY) | **CASO ESPECIAL** |

### ⚠️ CASO ESPECIAL: TIMEOUT en Pago

**NO Reintentar ciegamente. Primero CONSULTAR estado:**

```java
public PaymentResult procesarPago(Pago pago) {
    try {
        // 1. Enviar pago
        PaymentResponse response = paymentGateway.charge(pago);
        return PaymentResult.success(response.transactionId);
        
    } catch (TimeoutException e) {
        // 2. TIMEOUT → NO reintentar inmediatamente
        // Porque: El gateway PUDO procesar el pago antes de timeout
        
        logger.warn("TIMEOUT en payment gateway. Consultando estado...");
        
        // 3. Consultar estado (importante: usar Idempotency Key)
        PaymentStatus status = paymentGateway.queryStatus(
            pago.idempotencyKey  // Key única del pago
        );
        
        // 4. Tomar decisión según estado
        if (status == COMPLETED) {
            return PaymentResult.success(status.transactionId);
        } else if (status == PENDING) {
            // Webhook llegará después
            return PaymentResult.pending();
        } else {
            // FAILED → Reintentar CON NUEVO Idempotency Key
            throw new RetryablePaymentException();
        }
    }
}
```

### Patrón: **SAGA Coreografía + Compensación**

```
PASO 1: Crear Transacción
         │
         ▼
┌─────────────────────┐
│ transaction.state = │
│ PENDING             │
└────────┬────────────┘
         │
PASO 2: Procesar Pago
         │
    ┌────▼────────────────┐
    │ paymentGateway.charge│
    │ (with Idempotency Key)
    └────┬────┬────┬──────┘
         │    │    │
      SUCCESS FAIL TIMEOUT
         │    │    │
         ▼    ▼    ▼
         OK  ❌  QUERY
             │
    ┌────────┴──────────┐
    │                   │
 COMPENSATE       CHECK STATUS
 (Refund)         │
    │             ├─ COMPLETED ✅
    │             ├─ PENDING ⏳
    │             └─ FAILED ❌
    │
    ▼
ROLLBACK Transacción

PASO 3: Confirmar/Rechazar
         │
         ▼
transaction.state = COMPLETED or FAILED
```

### Configuración de Pagos:

```yaml
payment:
  consistency-level: STRONG
  
  gateway:
    timeout-ms: 3000
    idempotency-key-enabled: true
    
    retry-policy:
      # Solo para 5xx y TIMEOUT
      max-attempts: 3
      backoff-ms: 1000
      backoff-multiplier: 2.0
      
      # NO reintentar para 4xx
      dont-retry:
        - 400  # Bad Request
        - 402  # Insufficient Funds
        - 403  # Card Declined
        - 404  # Not Found
  
  # En caso de TIMEOUT
  timeout-handling:
    action: QUERY_THEN_DECIDE
    query-timeout-ms: 5000
    # Si QUERY también falla: conservador
    default-action: PENDING  # Esperar webhook
    
  # Webhook para confirmar pagos pendientes
  webhook:
    enabled: true
    timeout-ms: 30000
    retry-count: 5
```

---

## 📊 Resumen Comparativo

```
┌──────────────┬───────────┬──────────────┬─────────────────────┐
│ COMPONENTE   │ CAP       │ CONSISTENCIA │ NIVEL DE RIESGO     │
├──────────────┼───────────┼──────────────┼─────────────────────┤
│ CARRITO      │ AP        │ Eventual     │ BAJO (UX impact)    │
│ INVENTARIO   │ CP        │ Fuerte       │ ALTO (Oversell)     │
│ PAGOS        │ CP        │ Fuerte+Idem  │ CRÍTICO (Money)     │
└──────────────┴───────────┴──────────────┴─────────────────────┘
```

---

## 🚀 Patrones de Resiliencia Recomendados

| Componente | Patrón | Herramienta |
|-----------|--------|------------|
| **Carrito** | CQRS + Event Sourcing | Redis + Kafka |
| **Inventario** | Distributed Lock + Saga | Redlock + Saga |
| **Pagos** | Saga + Compensación + Webhook | Temporal/Axon |

---

## ✅ Checklist Implementación

- [ ] Carrito: Cache invalidation cada 5s
- [ ] Inventario: Distributed lock con TTL = 5s
- [ ] Inventario: Oversell guard en ANTES de commit
- [ ] Pagos: Idempotency Key en CADA request
- [ ] Pagos: Webhook handler para confirmación
- [ ] Pagos: Consultar estado ante TIMEOUT
- [ ] Observability: Métricas por componente
- [ ] Alertas: Cuando CAP compromisos se violen

---

## 🔍 Testing en Black Friday

```bash
# Simular race condition en inventario
k6 run --vus 100 --duration 60s test-inventory-race.js

# Simular timeout en payments
newman run payment-timeout-scenarios.postman_collection.json

# Verificar consistency en carrito
# Abrir 2 browsers → agregar items → verificar lag < 5s
```
