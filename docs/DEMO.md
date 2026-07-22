# Guion de la demostración

Duración objetivo: **10–12 minutos** de demo en vivo, alineada con los ítems de la rúbrica. Cada sección indica qué mostrar y qué decir. Ensayar al menos una vez completa antes de la presentación.

## Checklist previo (la noche anterior y 30 min antes)

- [ ] `docker compose ps` → los 14 contenedores arriba y `healthy`.
- [ ] Datos de demo presentes: período activo vigente, 2 bloques con casilleros (verificar en `/lockers`), al menos una renta previa (para que la tabla admin y el Excel no salgan vacíos).
- [ ] Dos usuarios listos: `admin` y un usuario estudiante **verificado** con contraseña conocida.
- [ ] Un ticket de ayuda ABIERTO del usuario estudiante (para no depender de crearlo en vivo).
- [ ] Sesiones iniciadas en **dos navegadores distintos** (no dos pestañas: se distinguen mejor en pantalla): estudiante en uno, admin en otro.
- [ ] Grafana abierta y logueada en el dashboard *Spring Boot 3.x Statistics*; pestaña extra en Explore con la query de Loki lista.
- [ ] Pestañas de GitHub abiertas: Actions (runs verdes) y Packages (imágenes GHCR).
- [ ] Consola Payphone: dominio `http://localhost:5173` autorizado y URL de respuesta correcta (si se hará pago real).
- [ ] Saldo/tarjeta de prueba disponible para el pago en vivo, o decisión tomada de mostrar el pago con la renta previa ya hecha (plan B).

## 1. Sistema corriendo en contenedores (~1 min)

**Mostrar**: `docker compose ps` en una terminal amplia.

**Decir**: "Todo el sistema — 5 servicios backend, 4 bases de datos, frontend y el stack de monitoreo — corre en 14 contenedores Docker orquestados con Compose. Los microservicios no publican puertos: solo se entra por el gateway (8080) y el frontend (5173)."

## 2. Flujo del estudiante: registro → renta → pago (~4 min)

En el navegador del **estudiante**:

1. (Opcional, si el tiempo alcanza) Registro de un usuario nuevo mostrando **validaciones en pantalla** (cédula de 10 dígitos, email, contraseña) y el **email de verificación** llegando de verdad.
2. Login → pantalla principal → **Casilleros**: grilla por bloques con leyenda de colores (verde disponible, rojo ocupado, amarillo pendiente, gris mantenimiento).
3. Rentar un casillero disponible: modal con renta por período ($6.50) o personalizada (precio calculado en vivo, $1.00/día).
4. **Cajita de Pagos de Payphone real** → pagar → redirección automática a "¡Pago exitoso!".
5. **Mis Alquileres**: la renta aparece ACTIVE. Volver a Casilleros: el casillero ahora está rojo.

**Decir mientras tanto**: "El frontend es una SPA React servida por nginx; consume exclusivamente APIs REST del gateway con JWT. El flujo de pago cruza tres servicios: rental-service reserva el casillero en locker-service con un lock pesimista, calcula el monto, y tras la confirmación de Payphone lo ocupa — con compensaciones si algo falla en el camino."

## 3. Panel de administración (~2 min)

En el navegador del **admin**:

1. Mostrar que el admin ve más opciones en la navbar (roles en el JWT).
2. **Rentas**: la renta recién creada, filtros por estado/fechas, y **Exportar Excel** (abrir el archivo descargado).
3. **Períodos** o **Bloques**: crear un bloque 1×3 y mostrar que aparecen 3 casilleros nuevos en la grilla.
4. (Rápido) En el navegador del estudiante, pegar la URL `/admin/periods` → redirige: la autorización se valida en el gateway **y** en el frontend.

## 4. WebSockets: chat de ayuda en tiempo real (~2 min)

1. Estudiante: abrir el ticket de ayuda preparado (o crear uno referenciando la renta).
2. Poner los dos navegadores **lado a lado**. Estudiante escribe → aparece al instante en el navegador del admin; admin responde → aparece al instante en el del estudiante.
3. Admin cierra el ticket → el estudiante ve el aviso de cierre en vivo y su input se deshabilita, **sin recargar**.

**Decir**: "Esto es STOMP sobre WebSocket, enrutado por el mismo gateway. La identidad del remitente sale del JWT validado en el handshake — el payload no puede suplantar a nadie — y un interceptor por frame impide suscribirse a chats ajenos o inyectar mensajes directos al broker. Los mensajes persisten en MongoDB: si recargo, el historial sigue" *(recargar para demostrarlo)*.

## 5. Circuit Breaker en vivo (~1.5 min) — el momento clave

1. En la terminal: `docker stop aeis-help-service-1`.
2. En el navegador: entrar a Ayuda → aparece el **error amable** ("El módulo de ayuda no está disponible..."), la app **no se cuelga** y todo lo demás sigue funcionando (navegar a Casilleros para probarlo).
3. `docker start aeis-help-service-1` → Ayuda vuelve a funcionar.

**Decir**: "Este es el patrón Circuit Breaker: el gateway detecta el fallo y corta el circuito respondiendo un fallback inmediato en vez de acumular requests colgados. El mismo patrón protege las llamadas internas de rental-service, con un detalle: los errores de negocio — casillero ocupado, 404 — están excluidos del conteo, solo los fallos de infraestructura abren el circuito."

## 6. Monitoreo y logs (~1.5 min)

1. **Grafana** → dashboard *Spring Boot 3.x Statistics*: selector con los 5 servicios, métricas de JVM/requests moviéndose por el tráfico de la demo.
2. **Explore → Loki**: `{container="aeis-rental-service-1"}` → mostrar los logs del pago que acabamos de hacer.
3. (Opcional) Prometheus `/targets`: 5/5 UP.

**Decir**: "Prometheus scrapea las métricas de cada servicio cada 15 segundos; Promtail envía los logs de todos los contenedores a Loki. Todo aprovisionado declarativamente: levantar el stack desde cero deja Grafana configurada sin un solo clic."

## 7. CI/CD (~1 min)

1. GitHub → **Actions**: el pipeline CI (matrix de 5 servicios + frontend, con tests) y CD en verde.
2. **Packages**: las 6 imágenes en GHCR con tags `latest` + SHA.

**Decir**: "Cada push corre build y tests de los 6 componentes; cada merge a main publica las imágenes versionadas en el registry. La demo que vieron corre exactamente estas imágenes."

## Plan B (si algo falla en vivo)

| Riesgo | Mitigación |
|---|---|
| Pago Payphone falla o no hay saldo | Mostrar la renta previa ya creada (checklist) y el flujo hasta la Cajita renderizada; explicar la confirmación con el diagrama de secuencia de [ARQUITECTURA.md](ARQUITECTURA.md) |
| Sin internet en el aula | Todo excepto Payphone y GitHub es local. Para CI/CD: capturas de pantalla de respaldo tomadas la noche anterior |
| Un contenedor no levanta | `docker compose restart <servicio>`; los healthchecks reordenan las dependencias solos |
| Email de verificación demora | Tener el usuario estudiante ya verificado (checklist); el registro en vivo es opcional |
| Se borraron los datos de demo | Recrear en 1 min: login admin → crear período + activar → crear bloque (o vía API, ver [API.md](API.md)) |
