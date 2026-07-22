# AEIS — Sistema de Alquiler de Casilleros (Microservicios)

Migración del monolito de referencia (`../monolithic-aeis-app`) a una arquitectura de
microservicios: API Gateway + Circuit Breaker, JWT, 4 microservicios Spring Boot
(auth, locker, rental, help) con base de datos propia cada uno, frontend React,
y monitoreo con Prometheus/Grafana/Loki.

La especificación completa del proyecto está en [`PLAN.md`](PLAN.md). Este README se
irá completando en la fase 11 (pulido y materiales de presentación) con diagramas y
guion de demo.

## Estado actual

🚧 **Fase 1 — Scaffolding.** Solo existe la estructura de carpetas y las 4 bases de
datos vía Docker Compose. El resto de servicios se implementa en las fases
siguientes del `PLAN.md` (§12).

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
