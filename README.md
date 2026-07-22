# AEIS — Sistema de Alquiler de Casilleros (Microservicios)

[![CI](https://github.com/Gabo0526/adv-web-apps-aeis-app/actions/workflows/ci.yml/badge.svg)](https://github.com/Gabo0526/adv-web-apps-aeis-app/actions/workflows/ci.yml)

Migración del monolito de referencia (`../monolithic-aeis-app`) a una arquitectura de
microservicios: API Gateway + Circuit Breaker, JWT, 4 microservicios Spring Boot
(auth, locker, rental, help) con base de datos propia cada uno, frontend React,
y monitoreo con Prometheus/Grafana/Loki.

La especificación completa del proyecto está en [`PLAN.md`](PLAN.md).

## Estado actual

✅ **Fases 1–9** — Scaffolding, los 4 microservicios backend + api-gateway, el
frontend React (auth/core + admin + ayuda con chat STOMP) y el stack de
monitoreo (Prometheus + Grafana + Loki/Promtail), todo corriendo en
Docker Compose.

✅ **Fase 10 — CI/CD y dockerización total.** `frontend/Dockerfile` multi-stage
(`node:20-alpine` → `nginx:alpine` con fallback SPA), servicio `frontend`
agregado al compose (`5173:80`), healthchecks + `depends_on: condition:
service_healthy` en cascada para los 5 servicios backend, workflows de GitHub
Actions (`ci.yml` con matrix de build+test backend/frontend, `cd.yml` con
build y push de las 6 imágenes a GHCR en cada push a `main`).

Pendiente: **fase 11** (pulido, diagramas comparativos y guion de demo — ver
`PLAN.md` §12).

## Arquitectura y URLs

```
Navegador ──► frontend (nginx :80 → host 5173)
Navegador ──► api-gateway :8080 ──► auth-service / locker-service / rental-service / help-service
Prometheus :9090 scrapea /actuator/prometheus de los 5 servicios backend
Grafana :3000 (datasources: Prometheus + Loki) · Loki :3100 · Promtail (logs Docker)
```

| Servicio | URL | Notas |
|---|---|---|
| Frontend | http://localhost:5173 | React servido por nginx |
| API Gateway | http://localhost:8080 | Rutas `/api/**`, WebSocket `/ws/**` |
| Grafana | http://localhost:3000 | usuario/clave: `admin`/`admin` (o los de `.env`) |
| Prometheus | http://localhost:9090 | |

Los servicios backend `auth-service` (8081), `locker-service` (8082),
`rental-service` (8083) y `help-service` (8084) **no publican puerto al host**:
solo son alcanzables desde dentro de la red Docker `aeis-net`, a través del
gateway (defensa en profundidad, ver `PLAN.md` §9).

## Requisitos

- Docker y Docker Compose
- (Solo si se quiere correr algún servicio fuera de Docker) Java 17, Maven, Node.js 20

## Arranque desde cero

1. Copiar la plantilla de variables de entorno y completar los valores reales
   (JWT secret, credenciales de BD, SMTP, Payphone, etc.):

   ```bash
   cp .env.example .env
   ```

2. Levantar todo el sistema (build de las 5 imágenes backend + frontend, y
   arranque de BDs/monitoreo):

   ```bash
   docker compose up -d --build
   ```

3. Verificar que todos los servicios queden `healthy`:

   ```bash
   docker compose ps
   ```

4. Abrir el frontend en **http://localhost:5173** (servido por nginx, no por
   `npm run dev`) e iniciar sesión con el usuario admin sembrado
   (`admin` / `ADMIN_DEFAULT_PASSWORD` de tu `.env`).

Para detener y limpiar los volúmenes:

```bash
docker compose down -v
```

## Puertos de desarrollo expuestos por las bases de datos

Las bases de datos publican un puerto al host **únicamente para poder
inspeccionarlas o correr un servicio backend fuera de Docker** durante
desarrollo (`mvn spring-boot:run`, un cliente MySQL/Mongo local, etc.). No son
necesarios para que el sistema funcione vía `docker compose up`, y podrían
volver a quedar internos sin afectar al resto del stack:

| Base de datos | Puerto host → contenedor | Uso |
|---|---|---|
| mysql-auth | 3307 → 3306 | `authdb`, inspección / `auth-service` local |
| mysql-locker | 3308 → 3306 | `lockerdb`, inspección / `locker-service` local |
| mysql-rental | 3310 → 3306 | `rentaldb`, inspección / `rental-service` local |
| mongo-help | 27017 → 27017 | `helpdb`, inspección / `help-service` local |

## Imágenes en GHCR

En cada push a `main`, `cd.yml` publica las 6 imágenes (tags `latest` y el SHA
del commit):

```
ghcr.io/gabo0526/adv-web-apps-aeis-app-api-gateway
ghcr.io/gabo0526/adv-web-apps-aeis-app-auth-service
ghcr.io/gabo0526/adv-web-apps-aeis-app-locker-service
ghcr.io/gabo0526/adv-web-apps-aeis-app-rental-service
ghcr.io/gabo0526/adv-web-apps-aeis-app-help-service
ghcr.io/gabo0526/adv-web-apps-aeis-app-frontend
```

## Estructura del repositorio

```
adv-web-apps-aeis-app/
├── .github/workflows/     # ci.yml (fase 10) + cd.yml (fase 10)
├── backend/
│   ├── api-gateway/        # fase 3
│   ├── auth-service/       # fase 2
│   ├── locker-service/     # fase 4
│   ├── rental-service/     # fase 5
│   └── help-service/       # fase 8
├── frontend/               # fases 6-7-10 (Dockerfile)
├── monitoring/             # fase 9
├── docker-compose.yml
├── .env.example
└── PLAN.md
```

## Desarrollo de servicios individuales

Las siguientes secciones documentan cómo levantar un servicio puntual fuera de
Docker (útil durante desarrollo activo de ese servicio). Para una demo
completa, usar `docker compose up -d --build` como se indica arriba.

### auth-service

`mysql-auth` publica el puerto `3307` del host (mapeado a `3306` del
contenedor) para poder correr `auth-service` localmente con
`mvn spring-boot:run` mientras se desarrolla.

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

### api-gateway

Enruta `/api/**` a los microservicios y valida el JWT emitido por
auth-service en cada petición protegida.

1. Levantar `mysql-auth`, `auth-service` y `api-gateway`:

   ```bash
   docker compose up -d --build mysql-auth auth-service api-gateway
   ```

2. Probar el flujo (login vía gateway, ruta protegida sin/con token):

   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"'"$ADMIN_DEFAULT_PASSWORD"'"}' \
     | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

   # Sin token -> 401
   curl -i "http://localhost:8080/api/users?page=0&size=5"

   # Con token -> 200, y el header X-User-Roles llega al servicio (visible en la respuesta paginada)
   curl -i "http://localhost:8080/api/users?page=0&size=5" -H "Authorization: Bearer $TOKEN"
   ```

3. Con `help-service` caído (`docker stop aeis-help-service-1`), la ruta
   `/api/help/**` demuestra el Circuit Breaker: cualquier request autenticado
   devuelve 503 con el mensaje de fallback en vez de colgarse.

Tests unitarios (reglas de autorización + validación de JWT, no requieren Docker):

```bash
cd backend/api-gateway
mvn test
```
