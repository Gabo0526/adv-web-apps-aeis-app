# Guía de desarrollo

Cómo trabajar en el proyecto más allá del `docker compose up` (correr un servicio fuera de Docker, tests, convenciones).

## Correr un servicio backend fuera de Docker

Útil durante desarrollo activo de un servicio (hot reload, debugger). Las bases de datos publican puertos al host **solo para esto**:

| Base de datos | Puerto host | Servicio que la usa |
|---|---|---|
| mysql-auth | 3307 | auth-service (8081) |
| mysql-locker | 3308 | locker-service (8082) |
| mysql-rental | 3310 | rental-service (8083) |
| mongo-help | 27017 | help-service (8084) |

Receta general (ejemplo con auth-service):

```bash
docker compose up -d mysql-auth
cd backend/auth-service
set -a && source ../../.env && set +a
mvn spring-boot:run
```

Los `application.properties` de cada servicio ya traen defaults que apuntan a `localhost:<puerto dev>`, así que basta con las variables del `.env` para credenciales/secretos.

> **Ojo con `source .env`**: valores con espacios sin comillas (como la app password de Gmail en `SPRING_MAIL_PASSWORD`) rompen el `source` en bash/zsh. Si el correo falla con `535 Authentication failed`, exporta esa variable a mano con comillas.

Si el servicio en desarrollo debe recibir tráfico del gateway dockerizado, recuerda que dentro de la red Docker el gateway busca `http://<servicio>:<puerto>`; lo más simple es reconstruir el contenedor al terminar: `docker compose up -d --build <servicio>`.

## Tests

- **Backend**: cada servicio tiene tests unitarios con JUnit 5 + Mockito y H2 en memoria (no requieren Docker): `mvn test` dentro de `backend/<servicio>`. Son los mismos que corre CI.
- **Frontend**: `npm run lint` (oxlint) y `npm run build` deben pasar sin errores; CI los exige.

## Convenciones del código

- **Backend**: paquete raíz `ec.edu.epn.fis.aeis.<servicio>`, capas `controller/service/repository/model/dto/exception`, errores JSON uniformes vía `@RestControllerAdvice`, configuración solo por variables de entorno con defaults (`${VAR:default}`). Los endpoints `/internal/**` jamás se enrutan en el gateway.
- **Frontend**: las 10 reglas de `PLAN.md` §8.8 (capa de API única con axios, AuthContext, hooks por feature sobre `useFetch`, componentes reutilizables, validadores centralizados, CSS por componente con tokens de `theme.css`). Antes de agregar una pantalla, revisar qué componentes ya existen en `src/components/`.
- **Commits**: convencionales (`feat(scope): ...`, `fix(scope): ...`), en español.

## Gestión de secretos

- `.env` (raíz) contiene TODOS los secretos y **nunca se commitea** (está en `.gitignore`). `.env.example` es la plantilla con los valores sensibles vacíos.
- El seed crea roles USER/ADMIN y un usuario `admin` con la contraseña de `ADMIN_DEFAULT_PASSWORD`.
- La contraseña de Grafana (`GRAFANA_ADMIN_PASSWORD`) solo aplica en el **primer arranque** de un volumen virgen; si se cambió después, alinearla con: `docker compose exec grafana grafana cli admin reset-admin-password "$GF_SECURITY_ADMIN_PASSWORD"`.

## Payphone en desarrollo

- La Cajita valida el **dominio** de la página que la renderiza: `http://localhost:5173` debe estar en los dominios autorizados de la consola Payphone.
- La **URL de respuesta** de la consola debe ser `http://localhost:5173/payment/result`.
- Los montos van en **centavos** (el backend ya los envía así en `amountCents`).
- El token de la Cajita es de uso en navegador (igual que en el monolito de referencia); la confirmación servidor-a-servidor usa el mismo token como Bearer en `POST /api/button/V2/Confirm`.

## Datos de demo

Con el stack arriba y el admin del seed (ver [API.md](API.md) para los curl):

1. `POST /api/periods` con fechas vigentes + `POST /api/periods/{id}/activate`.
2. `POST /api/locker-blocks` (p. ej. 2×5 con `allowCustomRental: true` y 2×4 sin).
3. Registrar un usuario estudiante desde el frontend y verificarlo por email.

> `docker compose down -v` **borra los volúmenes** (todos los datos). Sin `-v`, los datos sobreviven a un `down`/`up`.
