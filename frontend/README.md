# Frontend de pruebas

Este frontend estático permite probar el gateway y los endpoints del sistema desde `http://localhost:5173`.

## Inicio

1. Copia `.env.example` como `.env` en la raíz del proyecto.
2. Define un `JWT_SECRET` aleatorio de al menos 64 bytes.
3. Ejecuta `docker compose up --build`.
4. Abre `http://localhost:5173`.

El frontend consulta el gateway en `http://localhost:8080`. Además usa sus rutas internas de diagnóstico para comprobar la salud de los tres servicios.

## OAuth

Para Google o GitHub, cambia `SPRING_PROFILES_ACTIVE=oauth` y completa las cuatro variables del proveedor en `.env`. Registra estas callback URL en cada proveedor:

```text
http://localhost:8080/auth/login/oauth2/code/google
http://localhost:8080/auth/login/oauth2/code/github
```

Los botones de OAuth redirigen al gateway directamente para preservar la sesión necesaria para validar `state`.
