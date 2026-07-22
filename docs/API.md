# Referencia de API

Todas las rutas públicas se consumen **a través del gateway** (`http://localhost:8080`), que reescribe `/api/X/**` → `/X/**` hacia el servicio correspondiente. Salvo que se indique "pública", toda ruta exige `Authorization: Bearer <JWT>`; las marcadas **ADMIN** exigen además ese rol (validado en el gateway).

Formato de error uniforme en todos los servicios: `{"error": "mensaje"}` con el status HTTP correspondiente (400 validación, 401 no autenticado, 403 sin permisos, 404 no encontrado, 409 conflicto, 503 circuit breaker abierto).

## auth-service (vía `/api/auth`, `/api/users`)

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/register` | pública | Registro. Body: `{id (cédula, 10 dígitos), username, name, lastName, uniqueCode (9 dígitos), email, password, college}`. Envía email de verificación. 409 si usuario/email ya existen |
| `GET /api/auth/verify?token=` | pública | Consume el token de verificación → `{verified, message}` |
| `POST /api/auth/login` | pública | `{username, password}` → `{token, username, name, roles}`. 401 si la cuenta no está verificada |
| `POST /api/auth/forgot-password` | pública | `{email}` → **siempre 200** (anti-enumeración). Si existe, envía link de reseteo |
| `POST /api/auth/reset-password` | pública | `{token, newPassword}` → 200 / 400 (token inválido, expirado o usado) |
| `GET /api/users?page=&size=` | ADMIN | Usuarios paginados |
| `GET /api/users/search?idPrefix=` | ADMIN | Búsqueda por prefijo de cédula (usada en renta excepcional) |

## locker-service (vía `/api/lockers`, `/api/locker-blocks`)

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `GET /api/locker-blocks` | autenticado | Bloques con sus casilleros anidados (ordenados por número) — alimenta la grilla |
| `GET /api/lockers/block/{blockId}` | autenticado | Casilleros de un bloque |
| `POST /api/locker-blocks` | ADMIN | Crea bloque + genera `rows×columns` casilleros. Body: `{name, blockRows, blockColumns, periodId, allowCustomRental, lockerLength, lockerWidth, lockerHeight}` |
| `PUT /api/lockers/{id}` | ADMIN | Actualiza dimensiones (solo si AVAILABLE) |
| `PUT /api/lockers/{id}/toggle-maintenance` | ADMIN | AVAILABLE ↔ UNDER_MAINTENANCE |
| `DELETE /api/lockers/{id}` | ADMIN | Elimina (409 si no está AVAILABLE) |

Estados de casillero: `AVAILABLE` → `PENDING` (reservado durante el pago) → `OCCUPIED` (renta activa) → `AVAILABLE`; `UNDER_MAINTENANCE` fuera de circulación.

## rental-service (vía `/api/periods`, `/api/rentals`, `/api/payments`, `/api/excel`)

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `GET /api/periods` | autenticado | Todos los períodos |
| `GET /api/periods/active` | autenticado | Período activo |
| `POST /api/periods` | ADMIN | `{name, startDate, endDate}` (fechas ISO `yyyy-MM-ddTHH:mm:ss`) |
| `PUT /api/periods/{id}` | ADMIN | Editar período |
| `POST /api/periods/{id}/activate` | ADMIN | Activa este período y desactiva el resto |
| `POST /api/rentals/pre-rentals` | autenticado | Inicia la renta: `{lockerId, startDate?, endDate?}` → `{preRentalId, clientTransactionId, amountCents, reference, payphoneToken, payphoneStoreId}` (datos para la Cajita). Reserva el casillero; expira en 10 min |
| `GET /api/payments/payphone/confirm?id=&clientTransactionId=` | **pública** | Retorno del pago: confirma contra Payphone → `{success, message}`. Pública porque Payphone redirige al navegador antes de que la SPA cargue el token |
| `GET /api/rentals/mine` | autenticado | Rentas del usuario autenticado |
| `GET /api/rentals/admin?page=&size=` | ADMIN | Rentas paginadas |
| `GET /api/rentals/admin/filtered?...` | ADMIN | Con filtros (estado, usuario, rango de fechas) |
| `GET /api/rentals/admin/statuses` | ADMIN | Valores posibles de estado |
| `POST /api/rentals/admin/exceptional` | ADMIN | Renta manual: `{username, lockerId, startDate, endDate, amountPaid}` |
| `POST /api/excel/generate` | ADMIN | Exporta rentas a `.xlsx` (attachment) |

## help-service (vía `/api/help`, `/ws`)

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/help/tickets` | autenticado | `{subject, description, rentalRef?}` — el dueño sale de los headers de identidad |
| `GET /api/help/tickets/mine` | autenticado | Mis tickets |
| `GET /api/help/tickets` | ADMIN | Todos los tickets (abiertos primero) |
| `GET /api/help/tickets/{id}/messages` | dueño o ADMIN | Historial del chat |
| `PUT /api/help/tickets/{id}/close` | ADMIN | Cierra el ticket y emite el evento de cierre a los suscriptores |

### WebSocket (STOMP)

- **Conexión**: `ws://localhost:8080/ws?token=<JWT>` (el token va por query param porque los navegadores no permiten headers en el handshake WS; el gateway y el servicio lo validan).
- **Enviar mensaje**: frame SEND a `/app/tickets/{ticketId}/send` con body `{"content": "..."}`. La identidad del remitente se toma del token de la sesión, no del payload.
- **Recibir**: suscripción a `/topic/tickets/{ticketId}` — entrega `{id, ticketId, senderUsername, senderRole, content, sentAt}` y, al cierre del ticket, un evento con `ticketClosed: true`.
- **Reglas de autorización por frame**: SEND solo hacia `/app/**`; SUBSCRIBE a un ticket solo para su dueño o un ADMIN. Frames que violan las reglas reciben un frame ERROR y se cierra la conexión.

## Endpoints internos (NO expuestos por el gateway)

Solo alcanzables dentro de la red Docker `aeis-net`; los usa rental-service:

| Método y ruta | Servicio | Semántica |
|---|---|---|
| `PUT /internal/lockers/{id}/reserve` | locker-service | AVAILABLE→PENDING (lock pesimista) → `{id, number, blockName, allowCustomRental}`; 409 si no disponible |
| `PUT /internal/lockers/{id}/occupy` | locker-service | PENDING→OCCUPIED |
| `PUT /internal/lockers/{id}/release` | locker-service | PENDING/OCCUPIED→AVAILABLE |
| `GET /internal/users/{username}` | auth-service | `{id, username, name, lastName, email}` (snapshot para rentas) |

## Ejemplo de flujo completo por curl

```bash
# 1. Login (usuario seed admin)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<ADMIN_DEFAULT_PASSWORD>"}' | jq -r .token)

# 2. Ver bloques y casilleros
curl -s http://localhost:8080/api/locker-blocks -H "Authorization: Bearer $TOKEN" | jq

# 3. Iniciar una renta (reserva + datos para la Cajita)
curl -s -X POST http://localhost:8080/api/rentals/pre-rentals \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"lockerId": 1}' | jq

# 4. Ruta protegida sin token → 401 con error JSON
curl -i http://localhost:8080/api/users
```
