# Magadh Explora — Spring Boot Backend

REST API for the Magadh Explora Vite + React SPA. Spring Boot 3.5.14, Java 21, MySQL 8, JWT auth, Flyway migrations.

## What's in Phase 0

- JWT-secured REST API with role-based access (`ADMIN`, `USER`)
- Auth endpoints: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- Flyway baseline schema (`V1__init.sql`) — users, roles, packages, destinations, blogs, contacts, quotes, bookings, currency_rates, app_settings, translations, homepage_sections, homepage_section_items, plus join tables
- CORS configured for the Vite dev server (`http://localhost:5173`)
- Default admin user auto-created on first boot
- Dev / prod profiles in `application.yml`

## Prerequisites

- JDK 21 or newer (your machine has JDK 22 — fine)
- MySQL 8 running locally
- The Vite frontend at `H:\MagadhExplora\magadh_Explora` (already wired to `http://localhost:8080`)

## One-time setup

1. **Create the database** (Flyway will create the schema on first boot):

   ```sql
   CREATE DATABASE magadh_explora CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   The dev profile sets `createDatabaseIfNotExist=true`, so this step is optional if your MySQL user has `CREATE` privilege.

2. **Set environment variables** (PowerShell, current session):

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
   $env:DB_USER     = "root"
   $env:DB_PASSWORD = "your-mysql-password"
   $env:JWT_SECRET  = "replace-with-a-long-random-string-at-least-32-chars"
   $env:ADMIN_EMAIL = "admin@magadhexplora.local"
   $env:ADMIN_PASSWORD = "change-me-on-first-login"
   ```

   Defaults baked into `application.yml` work for quick local boot, but **change the JWT secret and admin password before any deploy**.

3. **(Optional) Mail config** — required only for Phase 4:

   ```powershell
   $env:MAIL_HOST     = "smtp.gmail.com"
   $env:MAIL_USERNAME = "you@gmail.com"
   $env:MAIL_PASSWORD = "app-password"
   $env:MAIL_FROM     = "no-reply@magadhexplora.local"
   $env:MAIL_ADMIN_TO = "you@gmail.com"
   ```

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

First boot will:
- Apply Flyway `V1__init.sql`
- Seed roles (`ADMIN`, `USER`), default `app_settings`, starter currency rates, default homepage sections
- Create the bootstrap admin user (logged with WARN: `Bootstrapped default admin user: ...`)

Server listens on `http://localhost:8080`.

## Smoke test

```powershell
# Register a user
curl -Method POST http://localhost:8080/api/auth/register `
     -ContentType "application/json" `
     -Body '{"name":"Test User","email":"test@example.com","mobile":"9999999999","password":"secret123"}'

# Login as admin
curl -Method POST http://localhost:8080/api/auth/login `
     -ContentType "application/json" `
     -Body '{"email":"admin@magadhexplora.local","password":"admin123"}'

# Use the returned token
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/auth/me
```

## Project layout

```
src/main/java/com/magadhexplora/api/
├── MagadhExploraApplication.java       application entrypoint
├── auth/                               login / register / me
│   ├── AuthController.java
│   ├── AuthService.java
│   └── dto/
├── config/
│   ├── SecurityConfig.java             JWT chain + CORS + role rules
│   ├── JwtProperties.java
│   ├── CorsProperties.java
│   ├── GlobalExceptionHandler.java
│   └── AdminBootstrap.java             default admin on first run
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── CustomUserDetailsService.java
└── user/
    ├── UserEntity.java / RoleEntity.java
    └── UserRepository.java / RoleRepository.java

src/main/resources/
├── application.yml                     dev / prod profiles
└── db/migration/V1__init.sql           baseline schema + seeds
```

## Phase roadmap

- **Phase 1** — Admin CRUD (packages, destinations, blogs, categories, homepage layout) + image uploads
- **Phase 2** — Geo-IP + currency conversion + admin markup %
- **Phase 3** — i18n (translations table + react-i18next on the frontend)
- **Phase 4** — Booking / contact / quote endpoints with Spring Mail
- **Phase 5** — Doc-driven feature work (pilgrimage/holiday toggle, traveler-type filter, custom builder, history page, SEO)
