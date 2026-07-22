# Plan de Implementación — Proyecto Final Aplicaciones Web Avanzadas

**Sistema de alquiler de casilleros AEIS-EPN: migración de monolito a microservicios.**

Este documento es la especificación completa de implementación. Está pensado para ejecutarse fase por fase sin contexto adicional. El monolito de referencia está en `../monolithic-aeis-app/` (Spring Boot 3.5.3, Java 17, Thymeleaf, MySQL, Payphone). Todo el proyecto nuevo se construye en este directorio (`adv-web-apps-aeis-app/`), que será su propio repositorio Git con remoto en GitHub.

---

## 0. Decisiones ya tomadas (NO reabrir)

| Tema | Decisión |
|---|---|
| Patrones de microservicios | **API Gateway** (Spring Cloud Gateway) + **Circuit Breaker** (Resilience4j) |
| Seguridad | **JWT HS256** con secreto compartido (env `JWT_SECRET`), emitido por auth-service, validado en el Gateway |
| Frontend | **React + Vite + react-router**, JavaScript (no TypeScript), estructura por features |
| Backend | 4 microservicios Spring Boot (Java 17) + Gateway. Se reutiliza el código del monolito adaptado |
| Bases de datos | Database-per-service: MySQL ×3 (auth, locker, rental) + MongoDB ×1 (help) |
| Monitoreo | Prometheus + Grafana (métricas) + Loki + Promtail (logs) |
| CI/CD | GitHub Actions (build, test, imágenes a GHCR). Demo con docker-compose local |
| Kubernetes | Descartado |
| Railway / nube | Descartado |
| WebSocket | Módulo "Ayuda" (help-service): tickets de soporte + chat STOMP. Lo más simple posible |
| Fuera de alcance | Intercambio de casilleros (`previousLockerRental`/`nextLockerRental` del monolito: ignorar). `azure-pipelines.yml`: ignorar |

---

## 1. Estructura del repositorio

```
adv-web-apps-aeis-app/
├── .github/workflows/
│   ├── ci.yml                  # build + tests en cada push/PR
│   └── cd.yml                  # build + push de imágenes Docker a GHCR en main
├── backend/
│   ├── api-gateway/            # Maven independiente (NO multi-módulo)
│   ├── auth-service/
│   ├── locker-service/
│   ├── rental-service/
│   └── help-service/
├── frontend/                   # React + Vite
├── monitoring/
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/   # datasources + dashboards
│   ├── loki/loki-config.yml
│   └── promtail/promtail-config.yml
├── docker-compose.yml
├── .env.example                # plantilla de variables (el .env real NO se commitea)
├── .gitignore
├── PLAN.md                     # este archivo
└── README.md                   # instrucciones de arranque + arquitectura
```

Cada servicio backend es un proyecto Maven **independiente** (pom propio con parent `spring-boot-starter-parent`), para que CI pueda construir cada uno por separado y cada uno tenga su Dockerfile. Paquete raíz sugerido: `ec.edu.epn.fis.aeis.<servicio>` (ej. `ec.edu.epn.fis.aeis.auth`).

Convenciones comunes a todos los servicios:
- Java 17, Spring Boot 3.5.x. Lombok igual que el monolito.
- Config por variables de entorno con defaults (`${VAR:default}`) en `application.properties`.
- Actuator + Micrometer Prometheus en TODOS los servicios: dependencias `spring-boot-starter-actuator` + `micrometer-registry-prometheus`, y `management.endpoints.web.exposure.include=health,info,prometheus`.
- Manejo de errores: `@RestControllerAdvice` por servicio devolviendo JSON `{"error": "mensaje"}` con el status correcto (400 validación, 401/403 auth, 404, 409 conflicto, 503 circuit breaker).
- Endpoints bajo `/internal/**` son SOLO para comunicación entre servicios: el Gateway nunca los enruta (quedan inaccesibles desde fuera de la red Docker).

---

## 2. Arquitectura y puertos

```
Navegador ──► frontend (nginx :80 → host 5173/80)
Navegador ──► api-gateway :8080  ──► auth-service   :8081 ── mysql-auth
                 │ (valida JWT)   ──► locker-service :8082 ── mysql-locker
                 │                ──► rental-service :8083 ── mysql-rental ──► Payphone API (externa)
                 │                ──► help-service   :8084 ── mongo-help
                 │                        ▲ ws/STOMP
                 └── rutas /ws/** ────────┘
rental-service ──REST interno──► locker-service (/internal/lockers/*)  [Circuit Breaker]
rental-service ──REST interno──► auth-service  (/internal/users/*)     [Circuit Breaker]
Prometheus :9090 scrapea /actuator/prometheus de los 5 servicios
Grafana :3000 (datasources: Prometheus + Loki) · Loki :3100 · Promtail (logs Docker)
```

---

## 3. api-gateway (Spring Cloud Gateway)

Dependencias: `spring-cloud-starter-gateway`, `spring-cloud-starter-circuitbreaker-reactor-resilience4j`, `jjwt` (io.jsonwebtoken, api+impl+jackson), actuator+micrometer. BOM de Spring Cloud compatible con Boot 3.5 (tren 2025.x).

### 3.1 Rutas

| Prefijo externo | Destino | Notas |
|---|---|---|
| `/api/auth/**` | auth-service | login, register, verify, forgot/reset: públicos |
| `/api/users/**` | auth-service | admin |
| `/api/lockers/**`, `/api/locker-blocks/**` | locker-service | |
| `/api/periods/**`, `/api/rentals/**`, `/api/excel/**`, `/api/payments/**` | rental-service | |
| `/api/help/**` | help-service | con Circuit Breaker + fallback (ver 3.4) |
| `/ws/**` | help-service (ws://) | handshake WebSocket; token JWT por query param |

Usar `StripPrefix=1` o reescritura para que los servicios reciban rutas sin `/api` (definir y ser consistente; recomendado: los servicios exponen `/auth/**`, `/lockers/**`, etc. y el gateway hace `RewritePath=/api/(?<segment>.*), /$\{segment}`).

### 3.2 Filtro JWT (GlobalFilter)

- Rutas públicas (sin token): `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/verify`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`, `GET /api/payments/payphone/confirm`, `/actuator/**` interno.
- Resto: exige `Authorization: Bearer <jwt>`. Para `/ws/**` acepta el token en query param `?token=` (los navegadores no permiten headers en el handshake WS).
- Validación: firma HS256 con `JWT_SECRET` y expiración. Si es válido, reenvía el request agregando headers `X-User-Id` (cédula), `X-Username`, `X-User-Roles` (CSV). **Siempre eliminar esos tres headers si vienen del cliente** (anti-spoofing).
- Autorización gruesa en el gateway: rutas que empiecen con `/api/users`, `/api/periods` (escritura), `/api/rentals/admin`, `/api/excel`, `/api/locker-blocks` (escritura), `/api/lockers` (escritura) requieren rol `ADMIN`. Mantener la lógica simple: mapa de (método, patrón) → rol requerido.

### 3.3 CORS

CORS global en el gateway: origen `http://localhost:5173` y `http://localhost:80` (configurable por env `CORS_ALLOWED_ORIGINS`), métodos GET/POST/PUT/DELETE/OPTIONS, headers `*`, credentials true.

### 3.4 Circuit Breaker en el gateway

Ruta de help-service con filtro `CircuitBreaker` (nombre `helpCB`, `fallbackUri: forward:/fallback/help`). Controlador local `/fallback/help` devuelve 503 `{"error":"El módulo de ayuda no está disponible en este momento. Intenta más tarde."}`. **Demo en la presentación**: `docker stop help-service` y mostrar la respuesta amable.

---

## 4. auth-service

Base: copiar y adaptar del monolito `UserService`, `CustomUserDetailsService`, `EmailService`, entidades `User`, `Role`, `VerificationToken`, enum `College`, repositorios correspondientes y `DatabaseInitializer`.

### 4.1 Modelo (MySQL `authdb`)

- `User`: igual al monolito (cédula como `id` String(10), username, name, lastName, uniqueCode(9), email, password BCrypt, college enum, enabled, roles ManyToMany). **Eliminar** la relación `lockerRentals` (ya no existe en este contexto).
- `Role`, `VerificationToken`: iguales al monolito.
- `PasswordResetToken` (**nuevo**): id, token UUID, user FK, expiration (1 hora), consumed boolean. (Alternativa aceptada: reutilizar VerificationToken con campo `type`; elegir lo que resulte más limpio.)
- Seed (`DatabaseInitializer`): roles USER y ADMIN + **un usuario ADMIN por defecto** (cédula `9999999999`, username `admin`, password desde env `ADMIN_DEFAULT_PASSWORD`, enabled=true) — el monolito no lo creaba y sin él no se puede demostrar nada de admin.

### 4.2 Endpoints públicos (vía gateway `/api/auth/**`)

| Método y ruta (en el servicio) | Request | Response | Notas |
|---|---|---|---|
| `POST /auth/register` | JSON: id, username, name, lastName, uniqueCode, email, password, college | 201 | Lógica de `UserService.register` del monolito. Valida duplicados (409). Envía email de verificación con link **`${FRONTEND_URL}/verify?token=...`** |
| `GET /auth/verify?token=` | — | 200 `{verified:true/false, message}` | Lógica de `verifyAccount` del monolito |
| `POST /auth/login` | `{username, password}` | 200 `{token, username, name, roles:[...]}` | Autentica con AuthenticationManager + BCrypt; usuario no `enabled` → 401 con mensaje de cuenta no verificada. Emite JWT |
| `POST /auth/forgot-password` | `{email}` | **siempre 200** `{message}` | Si el email existe, genera PasswordResetToken y envía link `${FRONTEND_URL}/reset-password?token=...`. Respuesta idéntica exista o no (anti-enumeración) |
| `POST /auth/reset-password` | `{token, newPassword}` | 200 / 400 | Valida token no consumido ni expirado, guarda BCrypt del nuevo password, marca consumido |

JWT: HS256 con `JWT_SECRET`; claims: `sub`=username, `uid`=cédula, `name`, `roles` (lista), `exp` = ahora + 8h. Sin refresh tokens (fuera de alcance).

### 4.3 Endpoints admin (gateway exige ADMIN)

- `GET /users?page=&size=` → página de UserDTO (copiar `getAllUsers` + `UserDTO`).
- `GET /users/search?idPrefix=` → lista de UserDTO (copiar `searchUsersByIdPrefix`). Lo usa la pantalla de renta excepcional.

### 4.4 Endpoint interno

- `GET /internal/users/{username}` → `{id, username, name, lastName, email}`. Lo consume rental-service.

### 4.5 Seguridad interna del servicio

`SecurityConfig` minimalista: **stateless, sin form-login, CSRF off, todo `permitAll`** (la autenticación/autorización real la hace el gateway; el servicio solo es alcanzable desde la red Docker). Conservar `PasswordEncoder` BCrypt. Los controladores que necesiten identidad leen los headers `X-User-Id`/`X-Username`/`X-User-Roles`.
Esta misma configuración de seguridad aplica a locker-service, rental-service y help-service.

### 4.6 Email

Copiar `EmailService` (spring-boot-starter-mail, SMTP Gmail por env). Nueva variable `FRONTEND_URL` (default `http://localhost:5173`) reemplaza al `getBaseUrl()` del monolito (eliminar toda la lógica de Azure/`WEBSITE_SITE_NAME`).

---

## 5. locker-service

Base: `Locker`, `LockerBlock`, enum `LockerStatus`, `LockerService`, `LockerBlockService`, repositorios, y los REST controllers `AdminLockerRestController`, `AdminLockerBlockRestController`, `UserLockerRestController` del monolito.

### 5.1 Modelo (MySQL `lockerdb`) — refactor clave

- `LockerBlock`: igual al monolito **pero** la relación `@ManyToOne Period period` se convierte en columna simple `periodId (Long, nullable=false)` — Period vive en rental-service. El frontend obtiene el período activo de rental-service y lo manda al crear el bloque.
- `Locker`: igual (number, status, length, width, height, FK a block).

### 5.2 Endpoints públicos (JWT requerido; escritura solo ADMIN)

| Método y ruta | Descripción |
|---|---|
| `GET /locker-blocks` | Lista de bloques con sus casilleros (para la vista de grilla del usuario y del admin) |
| `GET /lockers/block/{blockId}` | Casilleros de un bloque (copiar del monolito) |
| `POST /locker-blocks` | Crear bloque + generar sus `rows×columns` casilleros (lógica de `LockerBlockService`; body: name, blockRows, blockColumns, periodId, allowCustomRental, dimensiones default de lockers) |
| `PUT /lockers/{id}` | **Nuevo**: actualizar dimensiones de un casillero (solo si AVAILABLE) |
| `PUT /lockers/{id}/toggle-maintenance` | AVAILABLE ↔ UNDER_MAINTENANCE (copiar del monolito) |
| `DELETE /lockers/{id}` | **Nuevo**: eliminar casillero solo si status = AVAILABLE (409 si no) |

Los dos endpoints nuevos completan el **CRUD** exigido por el bimestre 1 (Create=crear bloque/casilleros, Read=listar/consultar, Update=editar+mantenimiento, Delete=eliminar).

### 5.3 Endpoints internos (solo rental-service)

| Método y ruta | Semántica |
|---|---|
| `PUT /internal/lockers/{id}/reserve` | Si status=AVAILABLE → PENDING y devuelve `{id, number, blockName, allowCustomRental}`; si no, 409 |
| `PUT /internal/lockers/{id}/occupy` | PENDING → OCCUPIED |
| `PUT /internal/lockers/{id}/release` | PENDING u OCCUPIED → AVAILABLE |

Estas tres operaciones sustituyen a las mutaciones directas de `Locker` que el monolito hacía dentro de `LockerPreRentalService`/`LockerRentalService`/schedulers. Usar lock pesimista en `reserve` como hacía `findByIdForUpdate`.

---

## 6. rental-service

Base: `Period`, `LockerPreRental`, `LockerRental`, enums `PreRentalStatus`, `RentalStatus`, `LockerRentalSetting`, servicios `PeriodService`, `LockerPreRentalService`, `LockerRentalService`, `RentalPreparation`, `RentalValidator`, `RentalCalculator`, `PayPhoneService`, `ExcelExportService`, `ExcelGenerator`, `LockerRentalSpecification`, schedulers `LockerPreRentalCleaner` y `LockerRentalCleaner`, y los controllers `AdminPeriodController`(→REST), `AdminLockerRentalRestController`, `AdminExcelExportRestController`, `UserLockerRentalController`(→REST), `PayPhoneWebhookController`(→REST).

### 6.1 Modelo (MySQL `rentaldb`) — refactor clave (desnormalización)

Las FKs a `User` y `Locker` del monolito se convierten en **IDs + snapshot de datos** (evita llamadas cruzadas en listados/Excel y es el argumento de "database per microservice" para la presentación):

- `Period`: igual al monolito (sin las relaciones `lockerBlocks`; conservar lógica de período activo único).
- `LockerPreRental`: reemplazar `@ManyToOne User` por `userId (String)` + `username`; reemplazar `@ManyToOne Locker` por `lockerId (Long)` + `lockerNumber (Integer)` + `blockName (String)` + `allowCustomRental (Boolean)` (datos que devuelve el `reserve`). Resto igual (createdAt, expiresAt, startDate, endDate, amountToPay, payPhoneClientTransactionId único, payPhoneTransactionId, status).
- `LockerRental`: reemplazar FKs user/locker igual que arriba (+ `userFullName` para el Excel); conservar FK local a `Period` y `lockerPreRental`; **eliminar** `previousLockerRental`/`nextLockerRental`. Campos: startDate, endDate, amountPaid, status.

Adaptar `LockerRentalSpecification`, DTOs y `ExcelGenerator` a los campos desnormalizados.

### 6.2 Clientes internos + Circuit Breaker (patrón #2)

`RestClient` hacia `http://locker-service:8082` y `http://auth-service:8081` (URLs por env). Anotar los métodos con `@CircuitBreaker(name="lockerService", fallbackMethod=...)` / `"authService"` (dependencia `resilience4j-spring-boot3` + AOP). Fallbacks lanzan excepción propia → `@RestControllerAdvice` la mapea a 503 `{"error":"Servicio de casilleros no disponible, intenta más tarde"}`. Config razonable: ventana 10, umbral 50%, wait 10s (en `application.properties`, mostrable en la presentación).

### 6.3 Flujo de renta (rediseño del flujo Payphone del monolito)

1. `POST /rentals/pre-rentals` (usuario autenticado). Body: `{lockerId, startDate, endDate?}`. El servicio: llama `reserve` al locker-service → valida período activo, fechas y soporte de renta custom (lógica de `RentalPreparation`/`RentalValidator`; si falla la validación, llama `release` para compensar) → calcula monto (`RentalCalculator`: precio de período 6.50 o custom 1.00/día, máx 15 días) → genera `clientTransactionId` UUID → crea el pre-rental (expira en 10 min) → **responde** `{preRentalId, clientTransactionId, amountCents, reference, payphoneToken, payphoneStoreId}` (los dos últimos desde env; el token de la Cajita es de uso en navegador, igual que en el monolito).
2. El frontend renderiza la **Cajita de Pagos** de Payphone con esos datos (ver §8.4).
3. Payphone redirige el navegador a la URL de respuesta configurada en su consola de desarrollador: **`${FRONTEND_URL}/payment/result?id=...&clientTransactionId=...`**.
4. La página React llama `GET /payments/payphone/confirm?id=&clientTransactionId=` (pública, vía gateway). El servicio ejecuta la lógica de `confirmPreRental` del monolito: confirma contra la API V2/Confirm de Payphone con el token backend; si `isPaid` → `occupy` en locker-service + crear `LockerRental` ACTIVE + pre-rental COMPLETED; si no → `release` + CANCELLED. Devuelve `{success, message}`. Si `occupy` falla tras un pago confirmado: reintentar 3 veces y loggear a nivel ERROR (compensación simple; sin Saga, decisión consciente).

### 6.4 Resto de endpoints

| Método y ruta | Rol | Base en el monolito |
|---|---|---|
| `GET /periods` | autenticado | `AdminPeriodRestController.all` |
| `GET /periods/active` | autenticado | `PeriodService.getActivePeriod` (lo usa el frontend para crear bloques) |
| `POST /periods` | ADMIN | form de `AdminPeriodController` → JSON `{name, startDate, endDate}` |
| `PUT /periods/{id}` | ADMIN | **nuevo** (editar nombre/fechas si no tiene rentas) |
| `POST /periods/{id}/activate` | ADMIN | igual al monolito (desactiva el resto) |
| `GET /rentals/admin?page=&size=` | ADMIN | `getAllLockerRentals` |
| `GET /rentals/admin/filtered?...` | ADMIN | `getFilteredLockerRentals` + `LockerRentalFilterDTO` |
| `GET /rentals/admin/statuses` | ADMIN | valores de `RentalStatus` |
| `POST /rentals/admin/exceptional` | ADMIN | `createExceptionalRental`: obtiene el usuario vía `GET /internal/users/{username}` a auth-service, reserva+ocupa el locker vía locker-service |
| `GET /rentals/mine` | usuario | **nuevo**: rentas del `X-User-Id` (para que el usuario vea su casillero y lo referencie en tickets de ayuda) |
| `POST /excel/generate` | ADMIN | `AdminExcelExportRestController` (devuelve el .xlsx como attachment) |

### 6.5 Schedulers

Portar `LockerPreRentalCleaner` (pre-rentals PENDING vencidos → EXPIRED + `release`) y `LockerRentalCleaner` (rentals ACTIVE con endDate pasado → COMPLETED + `release`), reemplazando mutaciones directas de Locker por llamadas al cliente interno. `@EnableScheduling` en la app.

---

## 7. help-service (nuevo — módulo de Ayuda con WebSocket)

Propósito: los estudiantes piden soporte cuando algo sale mal con su alquiler; conversan por chat en tiempo real con un admin. **Mantener mínimo.**

Dependencias: `spring-boot-starter-web`, `spring-boot-starter-websocket`, `spring-boot-starter-data-mongodb`, actuator+micrometer, jjwt (para validar el token del handshake).

### 7.1 Modelo (MongoDB `helpdb`)

- Colección `tickets`: `{id, userId, username, subject, description, rentalRef (String opcional, ej. "Casillero #12 - Bloque A"), status: OPEN|CLOSED, createdAt}`.
- Colección `messages`: `{id, ticketId, senderUsername, senderRole (USER|ADMIN), content, sentAt}`.

### 7.2 REST (vía gateway `/api/help/**`, JWT)

| Método y ruta | Rol | Descripción |
|---|---|---|
| `POST /help/tickets` | usuario | Crear ticket `{subject, description, rentalRef?}` (userId/username desde headers X-*) |
| `GET /help/tickets/mine` | usuario | Mis tickets |
| `GET /help/tickets` | ADMIN | Todos los tickets (OPEN primero) |
| `GET /help/tickets/{id}/messages` | dueño o ADMIN | Historial del chat |
| `PUT /help/tickets/{id}/close` | ADMIN | Cerrar ticket |

### 7.3 WebSocket/STOMP

- `@EnableWebSocketMessageBroker`; endpoint `/ws` (con SockJS opcional — preferible WS puro para simplificar el paso por el gateway); broker simple `/topic`; prefijo de aplicación `/app`.
- Cliente envía a `/app/tickets/{ticketId}/send` con `{content}`; el servidor persiste el mensaje (identidad tomada del token) y lo publica en `/topic/tickets/{ticketId}`.
- Autenticación: el gateway ya validó el token del query param en el handshake; el servicio además decodifica `?token=` en un `HandshakeInterceptor` para conocer username/rol del remitente (guardar en atributos de sesión WS).

---

## 8. frontend (React + Vite)

### 8.1 Setup y estructura

`npm create vite@latest frontend -- --template react`. Dependencias: `react-router-dom`, `axios`, `@stomp/stompjs`. CSS propio simple (variables + clases; se puede portar el estilo de `login.css` del monolito). Sin librería de UI pesada.

```
frontend/src/
├── api/client.js          # axios con baseURL VITE_API_URL y con interceptor JWT + manejo 401 (logout)
├── auth/                  # AuthContext (token+user en localStorage), ProtectedRoute, AdminRoute
├── features/
│   ├── auth/    Login.jsx Register.jsx ForgotPassword.jsx ResetPassword.jsx Verify.jsx
│   ├── home/    Home.jsx                       # pantalla principal post-login
│   ├── lockers/ LockerGrid.jsx RentModal.jsx   # grilla de bloques/casilleros + modal de renta
│   ├── payment/ PayphoneCheckout.jsx PaymentResult.jsx
│   ├── admin/   Periods.jsx CreateBlock.jsx Rentals.jsx ExceptionalRental.jsx Users.jsx
│   └── help/    MyTickets.jsx NewTicket.jsx TicketChat.jsx AdminTickets.jsx
├── components/  Navbar.jsx FormField.jsx ...
└── main.jsx / App.jsx     # definición de rutas
```

### 8.2 Rutas (cumplen las pantallas del bimestre 1)

| Ruta | Pantalla | Requisito 1B |
|---|---|---|
| `/login` | SignIn | 7a |
| `/register` | SignUp con selector de facultad (enum College) | 7b |
| `/forgot-password` y `/reset-password?token=` | Reseteo de clave | 7c ← **faltante del monolito** |
| `/verify?token=` | Resultado de verificación de cuenta | (flujo existente) |
| `/home` | Principal post-login: accesos a casilleros, mis rentas, ayuda; admin ve además su menú | 7d |
| `/lockers` | Grilla de bloques y casilleros con estado por color; casillero AVAILABLE → RentModal (fechas, precio calculado) | 7f (Read) |
| `/payment/checkout` | Cajita Payphone (estado navegación con datos del pre-rental) | — |
| `/payment/result` | Resultado del pago (llama al confirm) | — |
| `/admin/periods`, `/admin/blocks/new`, `/admin/rentals`, `/admin/rentals/exceptional`, `/admin/users` | CRUD y gestión admin | 7f (CRUD completo sobre casilleros/bloques/períodos) |
| `/help` (+ `/help/new`, `/help/:id`) | Tickets y **chat en tiempo real** | 7e ← **faltante del monolito** (WebSocket) |

Reglas: `ProtectedRoute` redirige a `/login` sin token; `AdminRoute` exige rol ADMIN; **Navbar** con navegación entre todas las pantallas y logout (requisito de navegabilidad, ítem 14 de la guía).

### 8.3 Validaciones en pantalla (ítem 13 de la guía — obligatorio)

Implementar en los formularios (con mensajes bajo el campo y bloqueo de submit):
- Registro: cédula requerida, solo dígitos, exactamente 10; código único solo dígitos, exactamente 9; username 4–30 `[a-zA-Z0-9_.]`; email formato válido; password mínimo 8 con confirmación.
- Períodos/bloques: nombres requeridos; rows/columns numéricos ≥1; fechas inicio < fin; dimensiones numéricas ≥0.
- Renta custom: rango de fechas ≤ 15 días, inicio no pasado.
- Renta excepcional: monto numérico ≥ 0.

### 8.4 Integración Cajita Payphone

En `PayphoneCheckout.jsx`: cargar dinámicamente `https://cdn.payphonetodoesposible.com/box/v1.1/payphone-payment-box.js` (+ su CSS) y renderizar `new PPaymentButtonBox({token, clientTransactionId, amount, amountWithoutTax, currency:"USD", storeId, reference}).render('pp-button')` — los valores vienen de la respuesta de `POST /rentals/pre-rentals` (§6.3; `amount` y `amountWithoutTax` en **centavos**). Botón "Cancelar" que vuelve a `/lockers`. **Nota operativa**: en la consola de Payphone debe configurarse la URL de respuesta hacia `${FRONTEND_URL}/payment/result`. Referencia visual: `../monolithic-aeis-app/src/main/resources/templates/user/locker-rental/payphone-checkout.html` y el PDF `../docs/Cajita de Pagos – Documentación oficial de Payphone.pdf`.

### 8.5 Chat STOMP

`TicketChat.jsx`: cliente `@stomp/stompjs` conectado a `ws://localhost:8080/ws?token=${jwt}` (URL por env `VITE_WS_URL`), suscripción a `/topic/tickets/{id}`, envío a `/app/tickets/{id}/send`, historial inicial por REST. Misma pantalla para usuario y admin.

### 8.6 Build/deploy

`Dockerfile` multi-stage: `node:20-alpine` (npm ci && npm run build) → `nginx:alpine` sirviendo `dist/` con fallback SPA (`try_files $uri /index.html`). Variables `VITE_API_URL` (`http://localhost:8080/api`) y `VITE_WS_URL` se inyectan en build (build-args en compose).

### 8.7 Guía visual (OBLIGATORIA — replica el estilo del monolito)

El frontend nuevo debe verse como una evolución del monolito, no como otra app. Referencias visuales para copiar el estilo: `../monolithic-aeis-app/src/main/resources/static/css/login.css` (tokens y login) y `templates/home.html`, `templates/locker-view.html` (navbar, tarjetas, grilla).

**Tokens globales** (crear `frontend/src/styles/theme.css`, importado en `main.jsx`; usar SIEMPRE las variables, nunca hex sueltos):

```css
:root {
  --primary-color: #2563eb;  --primary-dark: #1d4ed8;
  --success-color: #16a34a;  --danger-color: #dc2626;
  --warning-color: #ca8a04;  --info-color: #0891b2;
  --bg-light: #f8fafc;       --border-color: #e2e8f0;
  --text-main: #374151;      --text-muted: #6b7280;
  --shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1);
  --gradient-bg: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: var(--text-main); }
```

**Reglas por tipo de pantalla:**
- **Pantallas de auth** (login, register, forgot/reset, verify, checkout y result de pago): fondo `--gradient-bg` a pantalla completa, tarjeta blanca centrada (`border-radius: 20px`, `--shadow-xl`, padding 3rem, max-width 450–600px), título con ícono, botón primario ancho completo con gradiente azul (`linear-gradient(135deg, var(--primary-color), var(--primary-dark))`) que se eleva al hover (`translateY(-2px)` + sombra).
- **Pantallas internas** (home, lockers, admin, ayuda): fondo `--bg-light`, **navbar blanca superior fija** con logo "AEIS" a la izquierda (ícono `fa-archive` + texto), links con ícono al centro/derecha (Inicio, Casilleros, Mis Alquileres, Ayuda; + Períodos, Rentas, Usuarios si es ADMIN) y botón de logout; link activo en `--primary-color` con subrayado grueso. Contenido en tarjetas blancas (`border-radius: 12–16px`, `--shadow`, padding 1.5–2rem) sobre el fondo claro, con encabezado de página (título + subtítulo `--text-muted`).
- **Grilla de casilleros**: por cada bloque una tarjeta con su nombre y una cuadrícula CSS Grid de `blockColumns` columnas; cada casillero es una celda cuadrada redondeada con el número centrado, coloreada por estado: AVAILABLE verde (`--success-color`, clickeable, hover se eleva), OCCUPIED rojo (`--danger-color`), PENDING amarillo (`--warning-color`), UNDER_MAINTENANCE gris (`#9ca3af`). Leyenda de colores arriba de la grilla. Click en AVAILABLE abre el modal de renta.
- **Tablas admin**: dentro de tarjeta blanca, cabecera con fondo `--bg-light`, filas con hover, chips de estado con los mismos colores de la grilla, paginación con botones redondeados.
- **Formularios**: labels arriba, inputs con borde `--border-color` y `border-radius: 8px`, focus con borde `--primary-color`; error de validación en texto `--danger-color` pequeño bajo el campo. Botones secundarios blancos con borde; peligro en `--danger-color`.
- **Chat de ayuda**: burbujas redondeadas — usuario a la derecha en `--primary-color` con texto blanco, contraparte a la izquierda en `--bg-light`; input fijo abajo con botón de enviar (`fa-paper-plane`).
- **Íconos**: Font Awesome 6 vía CDN en `index.html` (igual que el monolito). **Feedback**: spinners `fa-spinner fa-spin` durante cargas y mensajes de éxito/error en banners redondeados (verde/rojo suaves: fondos `#f0fdf4`/`#fef2f2`).

### 8.8 Estándares de código frontend (OBLIGATORIOS)

El monolito tenía un frontend caótico (HTML de 1700 líneas con CSS/JS inline). El frontend nuevo se construye "con todas las de ley". Reglas no negociables:

1. **Capa de API única**: `api/client.js` exporta la instancia axios (baseURL `VITE_API_URL`, interceptor request que agrega `Authorization: Bearer` si hay token, interceptor response que ante 401 hace logout y redirige a /login). Sobre ella, un módulo por servicio: `api/authApi.js`, `api/lockersApi.js`, `api/rentalsApi.js`, `api/helpApi.js` con funciones nombradas (`login(credentials)`, `getLockerBlocks()`, …). **Prohibido** usar axios/fetch directamente en componentes.
2. **AuthContext único** (`auth/AuthContext.jsx`): guarda token + usuario (claims decodificados: username, name, roles) en estado y localStorage; expone `login()`, `logout()`, `isAdmin`. Hook `useAuth()`. Nadie más lee/escribe localStorage.
3. **Rutas centralizadas** en `App.jsx`, con constantes de paths en `routes.js` y wrappers `ProtectedRoute` / `AdminRoute` (redirigen a /login o /home). Prohibido hardcodear paths en los componentes: importar de `routes.js`.
4. **Carga de datos con hooks**: patrón `{data, loading, error}` mediante hooks por feature (`useLockerBlocks()`, `useMyRentals()`, …) construidos sobre un `useFetch` genérico. Prohibido repetir `useEffect` + estados de carga a mano en cada pantalla. Toda pantalla muestra spinner en loading y Banner de error en error — nunca pantalla en blanco ni `alert()`.
5. **Componentes reutilizables** en `components/`: `FormField` (label + input + mensaje de error), `Button` (variantes primary/secondary/danger, estado loading), `Card`, `Modal`, `Spinner`, `Banner` (success/error), `StatusChip` (colores según §8.7), `PageHeader`, `DataTable` (columnas + paginación). Los formularios y tablas de TODAS las pantallas se arman con estos componentes; prohibido duplicar su markup.
6. **Validación centralizada**: `utils/validators.js` con funciones puras (cédula 10 dígitos, uniqueCode 9, username, email, password≥8, rangos de fechas ≤15 días, montos) + hook ligero `useForm` (values, errors, touched, handleSubmit) que las aplica según §8.3. Prohibido validar inline distinto en cada formulario.
7. **Constantes de dominio** en `utils/constants.js`: enums LockerStatus/RentalStatus/PreRentalStatus/College con su label en español y su color (→ `StatusChip` y leyenda de la grilla). Prohibido comparar contra strings mágicos regados por el código.
8. **CSS ordenado**: `styles/theme.css` (tokens §8.7 + estilos base) y un `.css` por componente/página con clases prefijadas por el componente (`.locker-grid__cell`). Estilos inline SOLO para valores dinámicos (p. ej. `gridTemplateColumns` según `blockColumns`, color según estado).
9. **Estado global**: solo Context (auth). Nada de Redux/Zustand/react-query — innecesario a esta escala.
10. **Higiene**: componentes PascalCase en `.jsx`, hooks `useX`, componentes de máximo ~200 líneas (extraer subcomponentes), sin código muerto ni `console.log`, `npm run lint` (ESLint del template de Vite) sin errores, y `label` asociado a cada input (accesibilidad básica).

---

## 9. docker-compose.yml

Servicios (todos en una red `aeis-net`; healthchecks en las BDs y `depends_on: condition: service_healthy`):

| Servicio | Imagen/Build | Puertos host | Notas |
|---|---|---|---|
| mysql-auth / mysql-locker / mysql-rental | mysql:8.4 | solo internos | DB `authdb`/`lockerdb`/`rentaldb`, credenciales por env, volumen nombrado cada una |
| mongo-help | mongo:7 | interno | volumen |
| api-gateway | build backend/api-gateway | 8080:8080 | |
| auth-service | build backend/auth-service | interno | env SMTP, JWT_SECRET, FRONTEND_URL, ADMIN_DEFAULT_PASSWORD |
| locker-service | build backend/locker-service | interno | |
| rental-service | build backend/rental-service | interno | env PAYPHONE_TOKEN, PAYPHONE_STORE_ID, URLs internas |
| help-service | build backend/help-service | interno | |
| frontend | build frontend | 5173:80 | |
| prometheus | prom/prometheus | 9090 | monta `monitoring/prometheus/` |
| grafana | grafana/grafana | 3000 | provisioning montado; admin/admin |
| loki | grafana/loki | 3100 | |
| promtail | grafana/promtail | — | monta `/var/lib/docker/containers` y `/var/run/docker.sock` (ro) |

Dockerfile backend (igual para los 5): multi-stage `maven:3.9-eclipse-temurin-17` (`mvn -q package -DskipTests`) → `eclipse-temurin:17-jre`, `EXPOSE`, `ENTRYPOINT java -jar app.jar`.

`.env.example` con TODAS las variables (secretos vacíos). Los servicios internos **no publican puertos** al host (defensa en profundidad + argumento de seguridad para la presentación).

---

## 10. Monitoreo (Prometheus + Grafana + Loki)

- `prometheus.yml`: job `spring-services` con targets estáticos `api-gateway:8080`, `auth-service:8081`, `locker-service:8082`, `rental-service:8083`, `help-service:8084`, `metrics_path: /actuator/prometheus`, scrape 15s.
- Grafana provisioning: datasource Prometheus (`http://prometheus:9090`) + Loki (`http://loki:3100`); dashboard JSON pre-descargado de la comunidad ("Spring Boot 3.x Statistics", ID 19004 de grafana.com — descargar el JSON y commitearlo en `monitoring/grafana/provisioning/dashboards/`).
- Promtail: config estándar para logs de contenedores Docker (job `docker`, labels por nombre de contenedor).
- Demo prevista: dashboard con tráfico + en Explore/Loki filtrar logs de `rental-service` durante una renta.

---

## 11. CI/CD (GitHub Actions)

Repositorio GitHub público nuevo para `adv-web-apps-aeis-app` (requisito: commits de todos los integrantes).

- **`ci.yml`** — `on: [push, pull_request]`:
  - Job `backend`: matrix sobre `[api-gateway, auth-service, locker-service, rental-service, help-service]`; `actions/setup-java` (temurin 17, cache maven); `mvn -B verify` en `backend/${{ matrix.service }}`. Portar los tests unitarios de servicios del monolito que apliquen (UserService, PeriodService, RentalCalculator, RentalValidator, LockerService, LockerBlockService, PayPhoneService…) adaptados a los nuevos servicios, con H2 para tests como hace el monolito (`application-test.properties`).
  - Job `frontend`: setup-node 20, `npm ci`, `npm run build`.
- **`cd.yml`** — `on: push: branches [main]`: login a GHCR con `GITHUB_TOKEN`; matrix build+push de las 6 imágenes (`docker/build-push-action`), tags `latest` y `${{ github.sha }}`.
- Badge de CI en el README.

---

## 12. Fases de implementación (orden estricto)

Cada fase termina con su criterio de aceptación verificado antes de continuar.

1. **Scaffolding**: estructura de carpetas, `git init`, `.gitignore` (target/, node_modules/, .env, dist/), `.env.example`, README inicial, compose solo con las 4 BDs. ✅ `docker compose up` levanta las BDs sanas.
2. **auth-service completo** (registro, verify, login+JWT, forgot/reset, admin users, internal, seed, tests). ✅ Con la BD en Docker y el servicio local: registro→email→verify→login devuelve JWT válido; reset funciona.
3. **api-gateway** (rutas, filtro JWT, CORS, fallback help). ✅ Login vía `:8080/api/auth/login`; ruta protegida sin token → 401; con token → pasa con headers X-*.
4. **locker-service** (CRUD + internos + tests). ✅ CRUD completo vía gateway con token ADMIN; `reserve/occupy/release` responden solo desde la red interna.
5. **rental-service** (períodos, flujo pre-rental/confirm con Payphone, excepcional, filtros, Excel, schedulers, circuit breaker, tests). ✅ Flujo de renta completo con Payphone en modo prueba; con locker-service apagado, crear pre-rental devuelve 503 amable (CB abierto).
6. **frontend fases auth+core**: login, register, forgot/reset, verify, home, navbar, grilla de lockers, renta+checkout+result, validaciones. ✅ Un usuario nuevo se registra, verifica, loguea, renta y paga desde el navegador.
7. **frontend admin**: períodos, bloques, rentas (filtros+Excel), excepcional, usuarios. ✅ Todas las operaciones admin desde UI.
8. **help-service + UI de ayuda** (tickets REST + chat STOMP por el gateway). ✅ Usuario y admin chatean en tiempo real en dos navegadores.
9. **Monitoreo** (prometheus, grafana provisionada, loki/promtail). ✅ Dashboard muestra los 5 servicios; logs visibles en Grafana.
10. **CI/CD + dockerización total**: Dockerfiles, compose completo, workflows verdes en GitHub. ✅ `docker compose up --build` levanta TODO el sistema funcional desde cero; Actions en verde publica imágenes en GHCR.
11. **Pulido y materiales de presentación**: README con diagramas (monolito vs microservicios, comparativo — se generan al final), guion de demo (incluye demo del Circuit Breaker), verificación final contra la matriz §13.

---

## 13. Matriz de trazabilidad de requisitos

### Bimestre 1 (pantallas/funcionalidad base que deben existir)

| Requisito | Dónde se cumple |
|---|---|
| SignIn con contraseña encriptada en BD | `/login` + auth-service (BCrypt) |
| SignUp | `/register` + verificación por email |
| Reseteo/Olvidé clave | `/forgot-password`, `/reset-password` (**nuevo**) |
| Pantalla principal post-login | `/home` |
| Chat con WebSockets | `/help/:id` + help-service STOMP (**nuevo**) |
| CRUD desde la principal | CRUD de casilleros/bloques/períodos (Create/Read/Update/Delete §5.2, §6.4) |
| Conexión a BD relacional o NoSQL | MySQL ×3 + MongoDB |
| Validaciones en pantalla + navegabilidad | §8.3 + Navbar/rutas |

### Bimestre 2 (rúbrica RequisitosProyecto2B)

| Ítem rúbrica | Dónde se cumple |
|---|---|
| Diagrama comparativo monolito vs microservicios + explicación refactor backend | README/presentación (fase 11); evidencia: mapeo §4–§7 vs monolito |
| Refactorización de BD por microservicio | §5.1 y §6.1 (periodId plano, desnormalización user/locker) — explicar en presentación |
| 2+ patrones de microservicios | API Gateway (§3) + Circuit Breaker (§3.4, §6.2), ambos demostrables en vivo |
| Seguridad con ejemplos (JWT/OAuth) | JWT HS256 end-to-end (§3.2, §4.2), anti-spoofing de headers, BCrypt, anti-enumeración en reset |
| Frontend funcional conectado a micros + contenedores | React vía gateway; nginx en Docker (§8.6, §9) |
| Backend microservicios + contenedores | 5 contenedores backend + 4 BDs (§9) |
| Monitoreo y logs | Prometheus + Grafana + Loki (§10) |
| DevOps CI/CD | GitHub Actions (§11) |
| Demostración exitosa | Fase 11: guion de demo |
| Kubernetes (opcional) | No se hace (decisión registrada) |

---

## 14. Advertencias para quien implemente

1. **No copiar el `.env` del monolito al repo nuevo**; contiene credenciales reales. Usar `.env.example` y pedir los valores al usuario.
2. El monolito usa sesiones de Spring Security; NO portar `formLogin`/`SecurityConfig` tal cual — la seguridad nueva es stateless (§4.5).
3. Los montos Payphone van en **centavos** (multiplicar por 100 como hace `UserLockerRentalController:61`).
4. `LockerRentalSetting` (6.50 período / 1.00 día / máx 15 días) se conserva igual.
5. Los emails deben apuntar a `FRONTEND_URL`, nunca al gateway.
6. Al portar tests, mantener H2 y el patrón de `application-test.properties` del monolito.
7. Si Payphone no está disponible en la demo, dejar `PAYPHONE_TOKEN` de sandbox y documentar en README cómo simular la confirmación (llamar al confirm con un `clientTransactionId` inexistente muestra el manejo de error; para la demo real usar la cuenta sandbox del grupo).
8. Commits: mensajes convencionales (`feat(auth): ...`), en español o inglés pero consistentes; el repo debe mostrar commits de todos los integrantes del grupo.
