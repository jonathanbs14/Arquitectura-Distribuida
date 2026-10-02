# Guía paso a paso: descarga, ejecución y prueba de endpoints

Esta guía permite descargar, iniciar y comprobar el Sistema de Crédito Distribuido con Docker y Postman. También explica cómo sustituir el `auth-service` base por la versión mejorada que incluye JWT, MFA y acceso con Google/GitHub.

## 1. Archivos necesarios

Descarga estos archivos entregados con el proyecto:

1. [sistema-credito-distribuido-final.zip](C:\Users\centu\Documents\Codex\2026-09-18\com\outputs\sistema-credito-distribuido-final.zip): proyecto distribuido completo.
2. [auth-service-google-github-oauth.zip](C:\Users\centu\Documents\Codex\2026-09-18\com\outputs\auth-service-google-github-oauth.zip): reemplazo opcional y recomendado del módulo `auth-service` para habilitar autenticación local, JWT, MFA, Google y GitHub.

> Importante: el primer ZIP funciona por sí solo para solicitudes y evaluaciones. Su `auth-service` es una versión base: `POST /auth/login` responde **501 Not Implemented**. Para probar autenticación real, aplica el segundo ZIP como se describe en la sección 3.

## 2. Requisitos previos

- Windows 10/11.
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) iniciado y en estado **Running**.
- Java 21, únicamente si se desean ejecutar pruebas Gradle fuera de Docker.
- Postman o Insomnia para las pruebas HTTP. También puede usarse PowerShell.
- Cuenta de Google Cloud y/o GitHub, solo para probar OAuth real.

Comprueba que Docker esté listo desde PowerShell:

```powershell
docker version
docker compose version
```

## 3. Descomprimir y preparar el proyecto

1. Extrae `sistema-credito-distribuido-final.zip` en una carpeta de trabajo, por ejemplo `C:\proyectos`.
2. La carpeta raíz resultante se llama `nuevorpoyecto`.
3. Abre PowerShell dentro de esa carpeta:

```powershell
Set-Location 'C:\proyectos\nuevorpoyecto'
```

### 3.1. Opcional: instalar el auth-service mejorado

Haz este paso si se van a probar registro, login, JWT, MFA, Google o GitHub.

1. Extrae `auth-service-google-github-oauth.zip` en una carpeta temporal. Contiene una carpeta llamada `auth-service`.
2. Conserva una copia de respaldo de `nuevorpoyecto\auth-service`.
3. Copia la carpeta `auth-service` extraída y reemplaza la carpeta `nuevorpoyecto\auth-service`.
4. En `docker-compose.yml`, reemplaza el bloque `environment` del servicio `auth-service` por el siguiente:

```yaml
environment:
  JWT_SECRET: ${JWT_SECRET}
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-local}
  GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}
  GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}
  GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID:-}
  GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET:-}
```

Para probar registro y login local, genera una clave JWT nueva y activa el perfil local. Ejecuta esto en **la misma ventana de PowerShell** desde la que se iniciará Docker:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = 'local'
```

No subas esa clave a Git ni la publiques. Cuando se cierre PowerShell, esta variable deja de estar disponible y debe volver a definirse.

## 4. Iniciar todos los contenedores

Desde la raíz `nuevorpoyecto`, ejecuta:

```powershell
docker compose up --build
```

La primera ejecución descarga imágenes y compila los módulos, por lo que puede tardar varios minutos. Mantén esta ventana abierta para ver los logs.

En otra ventana de PowerShell, revisa que los contenedores estén levantados:

```powershell
Set-Location 'C:\proyectos\nuevorpoyecto'
docker compose ps
```

El sistema expone estos puertos en la máquina local:

| Componente | Puerto | Uso |
|---|---:|---|
| API Gateway | 8080 | Entrada pública recomendada. |
| solicitud-service | 8081 | Prueba directa del servicio de solicitudes. |
| evaluacion-service | 8082 | Prueba directa del servicio de evaluaciones. |
| auth-service | 8083 | Prueba directa de autenticación. |
| Kafka | 9092 | Broker de eventos; no es un endpoint REST. |
| RabbitMQ | 5672 | Broker AMQP; no es un endpoint REST. |
| RabbitMQ Management | 15672 | Consola web de RabbitMQ. |

## 5. Comprobación de salud

Antes de hacer pruebas de negocio, verifica estos endpoints. Cada uno debe devolver HTTP `200` y `"status":"UP"`.

```powershell
Invoke-RestMethod 'http://localhost:8080/actuator/health' # Gateway
Invoke-RestMethod 'http://localhost:8081/actuator/health' # Solicitudes
Invoke-RestMethod 'http://localhost:8082/actuator/health' # Evaluaciones
```

Para el `auth-service` base, usa:

```powershell
Invoke-RestMethod 'http://localhost:8083/actuator/health'
```

Para el `auth-service` mejorado, que usa el prefijo `/auth`, usa cualquiera de estas dos opciones:

```powershell
Invoke-RestMethod 'http://localhost:8083/auth/actuator/health' # Directo
Invoke-RestMethod 'http://localhost:8080/auth/actuator/health' # A través del gateway
```

## 6. Prueba completa del flujo de crédito

Estas peticiones usan el gateway (`http://localhost:8080`), que es la forma recomendada para un cliente externo.

### 6.1. Crear una solicitud

```powershell
$payload = @{ monto = 12000.00; plazoMeses = 24 } | ConvertTo-Json
$solicitud = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/solicitudes' `
  -ContentType 'application/json' `
  -Body $payload

$solicitud | ConvertTo-Json
$solicitudId = $solicitud.id
```

Resultado esperado: HTTP `201 Created` y un JSON parecido a este:

```json
{
  "id": "uuid-generado",
  "monto": 12000.00,
  "plazoMeses": 24,
  "estado": "RECIBIDA",
  "creadaEn": "..."
}
```

### 6.2. Consultar la solicitud creada

```powershell
Invoke-RestMethod "http://localhost:8080/solicitudes/$solicitudId"
```

Resultado esperado: HTTP `200 OK` con la misma solicitud.

### 6.3. Consultar la evaluación

La evaluación es asíncrona: la solicitud se publica en Kafka y `evaluacion-service` la procesa. Por ello, un `404` inmediato significa normalmente que todavía está en proceso.

```powershell
for ($intento = 1; $intento -le 10; $intento++) {
  try {
    $evaluacion = Invoke-RestMethod "http://localhost:8080/evaluaciones/$solicitudId" -ErrorAction Stop
    $evaluacion | ConvertTo-Json
    break
  } catch {
    Write-Host "La evaluación aún no está disponible; reintento $intento de 10..."
    Start-Sleep -Seconds 2
  }
}
```

Resultado esperado para `monto: 12000` y `plazoMeses: 24`:

```json
{
  "solicitudId": "el-mismo-uuid",
  "aprobado": true,
  "motivo": "Cumple criterios básicos",
  "evaluadaEn": "..."
}
```

La política actual aprueba montos de hasta `50000` y plazos de hasta `60` meses. Prueba también este caso para obtener un rechazo:

```json
{
  "monto": 70000,
  "plazoMeses": 72
}
```

### 6.4. Casos de error útiles

| Petición | Resultado esperado |
|---|---|
| `POST /solicitudes` con `{"monto":0,"plazoMeses":24}` | `400 Bad Request`; monto debe ser positivo. |
| `POST /solicitudes` con `{"monto":12000,"plazoMeses":0}` | `400 Bad Request`; plazo debe ser positivo. |
| `GET /solicitudes/{uuid-inexistente}` | `404 Not Found`. |
| `GET /evaluaciones/{uuid-inexistente}` | `404 Not Found`. |

## 7. Todos los endpoints de negocio

| Método | Ruta mediante gateway | Ruta directa | Respuesta correcta |
|---|---|---|---|
| `POST` | `/solicitudes` | `http://localhost:8081/solicitudes` | `201 Created`. Crea una solicitud. |
| `GET` | `/solicitudes/{id}` | `http://localhost:8081/solicitudes/{id}` | `200 OK`. Consulta una solicitud. |
| `GET` | `/evaluaciones/{solicitudId}` | `http://localhost:8082/evaluaciones/{solicitudId}` | `200 OK` cuando el evento ya fue procesado. |
| `GET` | `/actuator/health` | `http://localhost:8080/actuator/health` | Salud del gateway. |
| `GET` | — | `http://localhost:8081/actuator/health` | Salud de solicitudes. |
| `GET` | — | `http://localhost:8082/actuator/health` | Salud de evaluaciones. |

`POST /solicitudes` acepta este cuerpo JSON:

```json
{
  "monto": 12000.00,
  "plazoMeses": 24
}
```

## 8. Pruebas con Postman

### 8.1. Crear un environment

En Postman crea un ambiente llamado `Credito local` con estas variables:

| Variable | Valor inicial |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `solicitudId` | Déjala vacía. |
| `accessToken` | Déjala vacía. |
| `userId` | Déjala vacía. |

### 8.2. Solicitud para crear crédito

- Método: `POST`
- URL: `{{baseUrl}}/solicitudes`
- Header: `Content-Type: application/json`
- Body → raw → JSON:

```json
{
  "monto": 12000.00,
  "plazoMeses": 24
}
```

En la pestaña **Tests** agrega:

```javascript
pm.test("Creada", () => pm.response.to.have.status(201));
pm.environment.set("solicitudId", pm.response.json().id);
```

Envía la petición. Postman guardará automáticamente el UUID en `{{solicitudId}}`.

### 8.3. Solicitudes para consultar datos

1. `GET {{baseUrl}}/solicitudes/{{solicitudId}}`
2. `GET {{baseUrl}}/evaluaciones/{{solicitudId}}`

En la segunda petición, espera entre dos y cinco segundos y vuelve a enviarla si inicialmente devuelve `404`.

### 8.4. Consultar RabbitMQ

Abre `http://localhost:15672` en el navegador e inicia sesión con:

- Usuario: `guest`
- Contraseña: `guest`

Esta consola permite observar el broker. El resultado de evaluación se publica en el exchange `credito.evaluaciones.v1` con la clave `evaluacion.completada`.

## 9. Endpoints del auth-service base

Si no se aplicó el ZIP de OAuth, estos son los únicos endpoints de autenticación presentes en el primer ZIP:

| Método | URL | Resultado actual |
|---|---|---|
| `GET` | `http://localhost:8080/auth/providers` | `200 OK`, lista informativa de proveedores de la versión base. |
| `POST` | `http://localhost:8080/auth/login` | `501 Not Implemented`; no realiza login real. |
| `GET` | `http://localhost:8083/actuator/health` | Salud del auth-service base. |

No uses esta variante para una demostración de OAuth: no valida tokens, no produce JWT y no tiene Google/GitHub implementados como autenticación real.

## 10. Endpoints del auth-service mejorado

Después de aplicar el ZIP de OAuth y reiniciar con `JWT_SECRET` configurado, usa estas rutas. La URL principal es `http://localhost:8080/auth` por gateway; puede reemplazarse por `http://localhost:8083/auth` para acceso directo.

| Método | Ruta | Cuerpo / uso | Resultado correcto |
|---|---|---|---|
| `POST` | `/auth/register` | Registro local. | `200 OK` con `token`, `refreshToken` y `user`. |
| `POST` | `/auth/login` | Email, contraseña y opcionalmente `mfaCode`. | `200 OK` con JWT o respuesta que indica MFA pendiente. |
| `POST` | `/auth/mfa/setup/{userId}` | Activa preparación MFA para un usuario. | `200 OK` con secreto y QR en Data URI. |
| `POST` | `/auth/mfa/verify` | Confirma el código TOTP de la app autenticadora. | `200 OK` con `success`. |
| `GET` | `/auth/oauth/google` | Inicia Google OAuth desde un navegador. | Redirecciones y, al final, JSON con JWT. |
| `GET` | `/auth/oauth/github` | Inicia GitHub OAuth desde un navegador. | Redirecciones y, al final, JSON con JWT. |
| `GET` | `/auth/actuator/health` | Verifica la salud del auth-service mejorado. | `200 OK`. |

### 10.1. Registro local y uso del JWT en Postman

Crea esta petición:

- Método: `POST`
- URL: `{{baseUrl}}/auth/register`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "email": "ana@example.com",
  "fullName": "Ana Pérez",
  "password": "Una-clave-local-de-prueba"
}
```

En **Tests** agrega:

```javascript
pm.test("Registro correcto", () => pm.response.to.have.status(200));
const auth = pm.response.json();
pm.environment.set("accessToken", auth.token);
pm.environment.set("userId", auth.user.id);
```

Para iniciar sesión con la misma cuenta:

```json
{
  "email": "ana@example.com",
  "password": "Una-clave-local-de-prueba"
}
```

La respuesta contiene:

```json
{
  "token": "jwt-de-acceso",
  "refreshToken": "jwt-de-refresco",
  "user": {
    "id": "...",
    "email": "ana@example.com",
    "fullName": "Ana Pérez",
    "mfaEnabled": false
  },
  "mfaRequired": false
}
```

En Postman se puede enviar el token con la pestaña **Authorization** → tipo **Bearer Token** → `{{accessToken}}`.

> Estado actual importante: el gateway del proyecto todavía no valida JWT. Enviar `Authorization: Bearer {{accessToken}}` prueba que el cliente puede transportar el token, pero por ahora no bloquea ni protege `/solicitudes` o `/evaluaciones`. La validación JWT en Gateway es una mejora pendiente.

### 10.2. Probar MFA

1. Ejecuta `POST {{baseUrl}}/auth/mfa/setup/{{userId}}`.
2. La respuesta incluye `secret` y `qrCodeUrl`. Registra el secreto o QR en Google Authenticator, Authy u otra app TOTP.
3. Obtén el código de seis dígitos mostrado por la app y envía:

```json
POST {{baseUrl}}/auth/mfa/verify
Content-Type: application/json

{
  "userId": "{{userId}}",
  "code": "123456"
}
```

4. Debe responder `{"success":true}`. A partir de ese momento, envía `mfaCode` al hacer `POST /auth/login`:

```json
{
  "email": "ana@example.com",
  "password": "Una-clave-local-de-prueba",
  "mfaCode": "123456"
}
```

## 11. Probar OAuth con Google y GitHub

OAuth requiere navegador porque el proveedor debe mostrar su pantalla de consentimiento. Postman no sustituye ese flujo; solo permite observar la primera redirección `302` si se desactiva **Automatically follow redirects**.

### 11.1. Configuración previa

1. Crea una aplicación web OAuth en Google Cloud y una OAuth App en GitHub.
2. Si se accederá a través del gateway, registra exactamente estas callback URL:

```text
http://localhost:8080/auth/login/oauth2/code/google
http://localhost:8080/auth/login/oauth2/code/github
```

3. Define las credenciales en la misma PowerShell que ejecutará Docker:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'oauth'
$env:GOOGLE_CLIENT_ID = 'valor-de-google'
$env:GOOGLE_CLIENT_SECRET = 'secreto-de-google'
$env:GITHUB_CLIENT_ID = 'valor-de-github'
$env:GITHUB_CLIENT_SECRET = 'secreto-de-github'
```

`JWT_SECRET` debe permanecer definido como se indicó en la sección 3.1. Reinicia el contenedor del auth-service para que use las nuevas variables:

```powershell
docker compose up -d --build --force-recreate auth-service api-gateway
```

### 11.2. Ejecutar el flujo

Abre una de estas direcciones en un navegador:

```text
http://localhost:8080/auth/oauth/google
http://localhost:8080/auth/oauth/github
```

El navegador será redirigido al proveedor. Tras aprobar el acceso, Spring Security procesa el callback, consulta el perfil y devuelve un JSON con los JWT de la aplicación. No invoques manualmente `/auth/login/oauth2/code/google` ni `/auth/login/oauth2/code/github`: esos endpoints reciben un código temporal firmado por el proveedor y son exclusivos del callback OAuth.

## 12. Verificación de compilación sin Docker

Para comprobar las pruebas unitarias desde la raíz del proyecto:

```powershell
.\gradlew.bat test --no-daemon
```

Se necesita Java 21. Si Gradle indica que no lo encuentra, configura `JAVA_HOME` apuntando a la instalación de Java 21 y abre una nueva terminal.

## 13. Problemas frecuentes

| Problema | Causa probable | Acción recomendada |
|---|---|---|
| `docker` no se reconoce o no conecta | Docker Desktop no está instalado o iniciado. | Inicia Docker Desktop y vuelve a ejecutar `docker version`. |
| Puerto `8080`, `8081`, `8082`, `8083`, `9092` o `15672` ocupado | Otra aplicación usa el puerto. | Cierra la aplicación, cambia el mapeo de puertos en `docker-compose.yml` o detén el contenedor anterior. |
| La evaluación permanece en `404` | Kafka, consumidor o servicio de evaluación todavía no están listos. | Revisa `docker compose logs evaluacion-service kafka` y espera unos segundos. |
| `POST /auth/login` devuelve `501` | Sigue en uso el auth-service base. | Aplica el ZIP OAuth siguiendo la sección 3.1 y vuelve a construir los contenedores. |
| Auth mejorado no inicia | Falta `JWT_SECRET` o tiene menos de 64 bytes. | Genera una clave nueva y exporta `JWT_SECRET` antes de `docker compose up`. |
| `redirect_uri_mismatch` en Google/GitHub | La URL registrada no coincide con la usada. | Verifica protocolo, host, puerto, ruta y barra final; usa las URL de la sección 11.1. |
| OAuth devuelve `401` | Credenciales del proveedor incorrectas, expiradas o perfil OAuth no activo. | Revisa las cuatro variables de cliente/secreto y `SPRING_PROFILES_ACTIVE=oauth`. |
| Enviar Bearer token no cambia el acceso a solicitudes | El gateway aún no valida JWT. | Es el comportamiento actual; implementar Resource Server en Gateway es el siguiente paso de seguridad. |

## 14. Detener el entorno

Para detener los contenedores manteniendo su estado actual:

```powershell
docker compose stop
```

Para eliminar los contenedores de prueba:

```powershell
docker compose down
```

`down` elimina los contenedores y, como las bases de esta configuración no usan un volumen persistente, se perderán los datos de prueba almacenados en ellos.
