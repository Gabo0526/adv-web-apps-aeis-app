# AEIS — Sistema de Alquiler de Casilleros (Microservicios)

Migración del monolito de referencia (`../monolithic-aeis-app`) a una arquitectura de
microservicios: API Gateway + Circuit Breaker, JWT, 4 microservicios Spring Boot
(auth, locker, rental, help) con base de datos propia cada uno, frontend React,
y monitoreo con Prometheus/Grafana/Loki.

La especificación completa del proyecto está en [`PLAN.md`](PLAN.md). Este README se
irá completando en la fase 11 (pulido y materiales de presentación) con diagramas y
guion de demo.

## Estado actual

✅ **Fase 1 — Scaffolding.** Estructura de carpetas y las 4 bases de datos vía Docker
Compose.

✅ **Fase 2 — auth-service.** Registro con verificación por email, login con JWT,
forgot/reset password, endpoints admin (`/users`, `/users/search`) e interno
(`/internal/users/{username}`), seed de roles + usuario admin, `SecurityConfig`
stateless, tests portados del monolito con H2.

El resto de servicios se implementa en las fases siguientes del `PLAN.md` (§12).

## Estructura del repositorio

```
adv-web-apps-aeis-app/
├── .github/workflows/     # CI/CD (fase 10)
├── backend/
│   ├── api-gateway/        # fase 3
│   ├── auth-service/       # fase 2
│   ├── locker-service/     # fase 4
│   ├── rental-service/     # fase 5
│   └── help-service/       # fase 8
├── frontend/               # fases 6-7
├── monitoring/             # fase 9
├── docker-compose.yml
├── .env.example
└── PLAN.md
```

## Requisitos

- Docker y Docker Compose
- (Fases posteriores) Java 17, Maven, Node.js 20

## Arranque (fase 1)

1. Copiar la plantilla de variables de entorno y completar los valores:

   ```bash
   cp .env.example .env
   ```

2. Levantar las bases de datos:

   ```bash
   docker compose up -d
   ```

3. Verificar que las 4 bases de datos estén sanas:

   ```bash
   docker compose ps
   ```

   Las 4 deben mostrar estado `healthy`: `mysql-auth`, `mysql-locker`,
   `mysql-rental`, `mongo-help`.

Para detener y limpiar los volúmenes:

```bash
docker compose down -v
```

## auth-service (fase 2)

`mysql-auth` publica el puerto `3307` del host (mapeado a `3306` del contenedor)
para poder correr `auth-service` localmente con `mvn spring-boot:run` mientras se
desarrolla. Cuando el servicio quede dockerizado y enrutado por el gateway
(fases 3/10), ese mapeo puede volver a quedar interno.

1. Levantar `mysql-auth`:

   ```bash
   docker compose up -d mysql-auth
   ```

2. Exportar las variables de `.env` y correr el servicio (usa los defaults de
   `application.properties`, que ya apuntan a `localhost:3307`):

   ```bash
   cd backend/auth-service
   set -a && source ../../.env && set +a
   mvn spring-boot:run
   ```

   > Nota: `source .env` en bash rompe valores con espacios sin comillas (como
   > `SPRING_MAIL_PASSWORD`). Si el correo falla con `535 Authentication failed`,
   > exportar las variables línea por línea en vez de `source .env` directo.

3. Probar el flujo (registro → verificación → login → JWT; reset de contraseña):

   ```bash
   curl -X POST http://localhost:8081/auth/register -H "Content-Type: application/json" \
     -d '{"id":"1712345678","username":"jperez","name":"Juan","lastName":"Perez","uniqueCode":"201512345","email":"tu-correo@example.com","password":"password123","college":"FIS"}'

   # El link de verificación llega al correo; o se puede leer el token desde la BD:
   docker exec aeis-mysql-auth-1 mysql -uauth_user -pauthpass authdb -N -B \
     -e "SELECT token FROM verification_token WHERE user_id='1712345678';"

   curl "http://localhost:8081/auth/verify?token=<TOKEN>"

   curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" \
     -d '{"username":"jperez","password":"password123"}'
   ```

Tests con H2 (no requieren Docker):

```bash
cd backend/auth-service
mvn test
```
