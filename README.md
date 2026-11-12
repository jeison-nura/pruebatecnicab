# MngLib

Plataforma de anime y manga construida como monorepo: frontend **Next.js** y backend de **microservicios** (Go + Rust) detrás de un API Gateway.

## Estructura

```
animetest/
  backend/    # Gateway (Go) + servicios: auth, catalog, content, activity
  mnglib/     # Frontend Next.js (App Router)
```

## Stack

### Frontend (`mnglib/`)

- Next.js (App Router) + React
- TypeScript + Tailwind CSS 4
- ShadCN UI + Lucide

### Backend (`backend/`)

- **Gateway** (Go): punto único de entrada, valida JWT, rate limit, CORS.
- **Auth** (Go): registro, login, refresh tokens rotativos, logout.
- **Catalog** (Rust/axum): anime, manga, búsqueda, listados, hero slides.
- **Content** (Go): proveedores/scans, uploads, streaming HLS y páginas de manga (+ worker de transcodificación).
- **Activity** (Go): listas personales, progreso, historial y perfil.
- Infraestructura local: PostgreSQL, NATS y MinIO vía Docker Compose.

Ver `backend/docs/ARCHITECTURE.md` para la arquitectura completa.

## Desarrollo

### Backend

```bash
cd backend
make up      # levanta postgres + nats + minio + todos los servicios + gateway
make smoke   # health + register + login de prueba
```

Gateway disponible en `http://localhost:8080`.

### Frontend

```bash
cd mnglib
cp .env.example .env.local     # dejar vacío = modo mock; apuntar a gateway para API real
npm install
npm run dev
```

Disponible en `http://localhost:3000`.

## Estado actual

- Frontend con vistas de home, búsqueda, perfil, my list, favoritos, notificaciones, login, player y manga reader.
- Funciona con datos mock o contra el gateway real.
- Backend: Auth, Catalog, Activity y Content desplegados vía compose; Content incluye worker de transcodificación.
- Streaming/páginas vía URLs firmadas contra MinIO (compatible S3).

## Documentación

- Arquitectura: `backend/docs/ARCHITECTURE.md`
- Frontend: `mnglib/README.md`
- Backend: `backend/README.md`

## Licencia

Proyecto privado — ver licencia en el repositorio.
