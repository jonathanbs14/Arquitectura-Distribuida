# Sistema de crédito distribuido

Este repositorio transforma el monolito original en un sistema de servicios independientes. Cada servicio se compila y despliega por separado, posee su propia base de datos y se comunica mediante contratos versionados.

## Arquitectura

```text
Cliente
   |
api-gateway :8080
   |--------------------------|----------------------|
solicitud-service :8081  evaluacion-service :8082  auth-service :8083
   |              |                  |                       |
PostgreSQL   Kafka (evento)      PostgreSQL + RabbitMQ      proveedor OIDC
```

1. `POST /solicitudes` llega a `solicitud-service`, que persiste la solicitud en **su propia** base de datos y registra el evento en una outbox transaccional.
2. El publicador de la outbox emite `credito.solicitud.creada.v1` a Kafka.
3. `evaluacion-service` consume ese contrato de forma idempotente, aplica la política de crédito y persiste el resultado en **su propia** base de datos.
4. La evaluación completada se publica desde otra outbox al exchange RabbitMQ `credito.evaluaciones.v1` con la clave `evaluacion.completada`.

La outbox evita perder un evento si el proceso cae entre la escritura en la base de datos y la publicación. Los consumidores deben tratar los eventos como *al menos una vez*; el servicio de evaluaciones ya deduplica por `solicitudId`.

## Módulos

- `contracts`: DTOs y eventos JSON versionados, sin lógica de infraestructura.
- `api-gateway`: único punto de entrada HTTP; enruta las rutas públicas a los servicios.
- `solicitud-service`: creación y consulta de solicitudes.
- `evaluacion-service`: evaluación asíncrona y consulta del resultado.
- `auth-service`: registro y login local, JWT HS512, MFA TOTP y OAuth2 opcional con Google/GitHub.
- `frontend`: consola web para probar la salud de los servicios y todos los endpoints públicos mediante el gateway.

## Ejecutar con Docker

Se necesita Docker Desktop. Desde la raíz del proyecto:

```powershell
# Copiar .env.example como .env y definir JWT_SECRET antes de iniciar.
Copy-Item .env.example .env
docker compose up --build
```

El frontend queda disponible en `http://localhost:5173`. Los servicios quedan disponibles mediante el gateway en `http://localhost:8080`. RabbitMQ Management está en `http://localhost:15672` (usuario y contraseña por defecto: `guest`).

Para generar una clave JWT segura en PowerShell:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copia el resultado como valor de `JWT_SECRET` en `.env`. Consulta [frontend/README.md](frontend/README.md) para probar los endpoints desde el navegador.

Crear una solicitud:

```powershell
$solicitud = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/solicitudes `
  -ContentType 'application/json' `
  -Body '{"monto":12000.00,"plazoMeses":24}'
```

Consultar el resultado (la evaluación es asíncrona; esperar unos segundos si devuelve 404):

```powershell
Invoke-RestMethod "http://localhost:8080/evaluaciones/$($solicitud.id)"
```

## Desarrollo local

El proyecto usa Java 21 y Gradle Wrapper. Para compilar y ejecutar las pruebas unitarias:

```powershell
.\gradlew.bat test
```

Para ejecutar infraestructura y un servicio de forma local, use Kafka en `localhost:9092`, RabbitMQ en `localhost:5672` y cambie el perfil de datos según corresponda. Cada servicio tiene H2 en memoria como valor por defecto para facilitar desarrollo, mientras que Docker Compose utiliza PostgreSQL separado.

## Seguridad

Los secretos se leen únicamente desde variables de entorno. No subas el archivo `.env`. El perfil `local` permite probar registro, login y MFA. Para OAuth, define `SPRING_PROFILES_ACTIVE=oauth`, registra las aplicaciones en Google/GitHub y usa estas callbacks por gateway:

```text
http://localhost:8080/auth/login/oauth2/code/google
http://localhost:8080/auth/login/oauth2/code/github
```

El frontend permite enviar el JWT en las peticiones, pero el gateway todavía no valida el token como Resource Server. Esa protección debe implementarse antes de un despliegue productivo.
