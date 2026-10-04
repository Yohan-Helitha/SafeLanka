# SafeLanka frontend

React 19 + TypeScript + Vite + Tailwind CSS 3, in the **Command dark theme**. It builds all 25 screens for the five roles and runs on an in-browser mock database that matches the Flyway seed, so no backend is needed to try it.

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # type-check + production build
npm run lint
```

## Mock data or the real API

One switch in your own `frontend/.env.local` (copy `.env.example`; with no file the app defaults to mocks):

```
VITE_USE_MOCKS=true                       # in-browser mocks, no backend
VITE_API_BASE_URL=http://localhost:8080/api
```

Set `VITE_USE_MOCKS=false` to call the Spring Boot API. Keep these settings in `.env.local` only: Vite gives `.env.development` a higher priority than `.env.local`, so a value committed there would silently override yours. Screens never know which one they get: they only use the `api` object from `src/services`. In mock mode the data lives in memory, so a full page reload resets it (saved offline items stay in IndexedDB).

## Login

`VITE_AUTH_MODE=login` (default) uses real accounts: `/login`, `/signup` (mobile number, optional email, SMS code on `/verify`), and Log out in the account menu. The access token lives in memory only; on page load and on any 401 the app renews it through the httpOnly refresh cookie (one shared call, because refresh tokens rotate). In mock mode the same flow runs in the browser; every seeded person logs in with the password `Demo@1234` (see the root README for the accounts) and the SMS code is shown on screen.

`VITE_AUTH_MODE=demo` brings back the role switcher with `X-Acting-User` for a safe demo (use with the backend profile `demo-auth`).

Against the real backend, set `VITE_USE_MOCKS=false` and `VITE_PROXY_TARGET=http://localhost:<port>` in `.env.local`. Keep `VITE_API_BASE_URL=/api`: the dev server proxies it, so the cookie stays same-origin and no CORS setup is needed.

## Folder structure

```
src/
├─ components/        UI building blocks
│  ├─ ui/             Button, Card, Field, Segmented, Dialog, DataTable, States, ...
│  ├─ domain/         SeverityBadge, StatusChip, OccupancyBar, ApiErrorNotice
│  ├─ layout/         PortalLayout, MobileLayout, SituationStrip, RoleSwitcher, Offline*
│  └─ reports/ warnings/ response/ analytics/    module-only components
├─ constants/         env, app limits, routes (paths), roles, labels
├─ context/           Toast, Auth, Outbox providers + AppProviders
├─ hooks/
│  ├─ shared/         useOnlineStatus, useReferenceData, useGeolocation, ...
│  └─ reports/ warnings/ response/ analytics/    React Query hooks per module
├─ navigation/        router.tsx, RequireRole, navConfig (sidebar / bottom-bar items per role)
├─ screens/
│  ├─ auth/           LoginScreen, SignupScreen, VerifyPhoneScreen
│  ├─ landing/        LandingRoute, RoleSelectScreen (demo mode), NotFoundScreen
│  └─ reports/ warnings/ response/ analytics/    one file per screen
├─ services/          the only data access: http client, ApiError, session, offline outbox
│  ├─ auth/ reference/ reports/ warnings/ response/ analytics/    <module>Api.ts, .http.ts, .mock.ts
│  └─ mocks/          seeded in-memory database, event bus, exporters
├─ theme/             global.css (CSS variables), tokens.ts (severity, status tones, chart colours)
├─ types/             DTO types per module
└─ utils/             format (Asia/Colombo dates), geo, id, audio, validation
```

Rules: screens import from `components/`, `hooks/`, `context/` and `services` (via hooks) only. Each module folder maps to one team member's backend module.

## Screens by role

| Role | Surface | Routes |
| --- | --- | --- |
| Citizen, volunteer | Light mobile | `/app` alerts, `/app/alerts/:id`, `/app/report`, `/app/report/done/:id`, `/app/reports` |
| Rescue team member | Light mobile | `/team` |
| Shelter coordinator | Light mobile | `/coordinator` |
| DMC duty officer | Dark portal | `/dmc/hazards`, `/dmc/hazards/:id`, `/dmc/reports`, `/dmc/reports/:id`, `/dmc/warnings`, `/dmc/warnings/new`, `/dmc/warnings/:id`, `/dmc/simulation`, `/dmc/analytics`, `/dmc/analytics/:id` |
| District officer | Dark portal | `/district`, `/district/teams`, `/district/assignments/new`, `/district/shelters`, `/district/relief`, `/district/analytics` (read-only), `/district/analytics/:id` |
| Everyone | Dark | `/login`, `/signup`, `/verify`, `/` (role picker in demo mode), 404 |

## Offline demo

Use the **Online / Offline** switch in the header. While offline, hazard reports, rescue status updates and relief distributions are saved on the device (IndexedDB) and sent automatically, oldest first, when the connection returns.

## Maps

Maps use Leaflet on OpenStreetMap tiles, so no API key or billing is needed (`components/ui/LocationMap`). They appear on report detail, the rescue assignment, the district shelters page, and the new-assignment form, where clicking the map sets the location. Tiles need internet; the OSM tile server is for light use, so move to a hosted tile provider before heavy production traffic.

## Secrets

Copy `.env.example` to `.env.local` (git-ignored). Every `VITE_*` value is bundled into the browser, so only public keys belong here. The only public key placeholder left is `VITE_PUSH_VAPID_PUBLIC_KEY`, for browser push later.
