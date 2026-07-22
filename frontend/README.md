# AEIS — Frontend

React + Vite (JavaScript). Consume el sistema de casilleros AEIS-EPN a través del api-gateway.

## Desarrollo

```bash
npm install
npm run dev
```

Variables de entorno (`.env`, no versionado):

```
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

## Estructura

Ver `PLAN.md` (raíz del repo) §8 para la especificación completa: capa de API (`api/`), autenticación (`auth/`), pantallas por feature (`features/`), componentes reutilizables (`components/`), hooks y utilidades (`hooks/`, `utils/`).

## Scripts

- `npm run dev` — servidor de desarrollo (puerto 5173)
- `npm run build` — build de producción
- `npm run lint` — oxlint
