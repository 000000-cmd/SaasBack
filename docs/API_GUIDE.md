# SaaS Platform — Guía Completa

> **Ámbito**: API + manejo de ambientes/perfiles + cómo cambiar entre IntelliJ y Docker.
> **Stack**: Spring Boot 3.5.9, Java 21, MySQL 8.4, Redis 7.4, Eureka, Spring Cloud Config, Spring Cloud Gateway.

---

## Tabla de contenidos

1. [Arquitectura y puertos](#1-arquitectura-y-puertos)
2. [Autenticación — flujo y JWT](#2-autenticación--flujo-y-jwt)
3. [Formato común de respuestas](#3-formato-común-de-respuestas)
4. [Errores estándar](#4-errores-estándar)
5. [Endpoints — Auth Service](#5-endpoints--auth-service)
6. [Endpoints — System Service](#6-endpoints--system-service)
7. [Endpoints — Gateway / Info](#7-endpoints--gateway--info)
8. [Endpoints internos (S2S)](#8-endpoints-internos-s2s)
9. [Manejo de ambientes (profiles)](#9-manejo-de-ambientes-profiles)
10. [Cambiar de modo: IntelliJ ↔ Docker](#10-cambiar-de-modo-intellij--docker)
11. [Cambiar de ambiente: local → dev → prod](#11-cambiar-de-ambiente-local--dev--prod)
12. [Rate limiting](#12-rate-limiting)
13. [Datos seed (cuenta admin, roles, etc.)](#13-datos-seed-cuenta-admin-roles-etc)

---

## 1. Arquitectura y puertos

```
                      ┌─────────────────┐
   Cliente ──────────▶│ Gateway (8080)  │  ← rutas explícitas, JWT validation, Redis rate-limit
                      └────────┬────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     ┌─────────────────┐ ┌─────────────────┐
     │ Auth (8082)     │ │ System (8083)   │
     │ Flyway + JWT    │ │ catálogos       │
     └────────┬────────┘ └────────┬────────┘
              │                   │
              ▼                   ▼
     ┌──────────────────────────────────┐
     │ MySQL (3306)  +  Redis (6379)    │
     └──────────────────────────────────┘

Infra:  Eureka Discovery (8761)   +   Config Server (8888)
```

| Servicio | Puerto | Rol |
|---|---|---|
| **gateway-service** | 8080 | Único punto de entrada. Valida JWT, inyecta `X-User-*`, rate-limit |
| **auth-service** | 8082 | Login/logout/refresh, usuarios, ejecuta Flyway al arrancar |
| **system-service** | 8083 | Roles, permisos, menús, listas del sistema, constantes |
| **discovery-service** | 8761 | Eureka — registro de servicios |
| **config-server** | 8888 | Sirve `saas-config-repo/*.properties` a todos los servicios |
| **mysql** | 3306 | BD `saas_db` |
| **redis** | 6379 | Cache + JWT blacklist + rate-limit |

### URLs base

| Modo | Cliente HTTP usa | Servicios entre sí usan |
|---|---|---|
| **IntelliJ** (local) | `http://localhost:8080` | `localhost` |
| **Docker** | `http://localhost:8080` | hostnames de contenedor (`mysql`, `redis`, etc.) |

> **Regla**: el frontend siempre llama al **gateway** (`localhost:8080`). Nunca a los servicios directos.

---

## 2. Autenticación — flujo y JWT

### Flujo

```
1. Cliente ──POST /auth/login──▶ Gateway ──▶ Auth
                                            │
2. Auth valida password (BCrypt)            │
3. Auth resuelve roles via Feign ──▶ System
4. Auth genera JWT con roles                │
5. Auth retorna {accessToken, refreshToken} │
                                            ▼
6. Cliente guarda tokens
7. Siguientes requests:  Authorization: Bearer <accessToken>
8. Gateway valida firma + verifica blacklist Redis
9. Gateway inyecta X-User-Id, X-User-Username, X-User-Roles al request
10. Servicio downstream confía y procesa
```

### Estructura del JWT (access token)

```json
{
  "sub": "e7bfb3c5-9fc6-4f79-b249-cacd9a9cb151",   // userId (UUID)
  "username": "admin",
  "roles": ["ADMIN"],                                // codes de rol
  "iat": 1761953477,
  "exp": 1761957077                                  // sub + 1h
}
```

| Token | TTL | Configurable en |
|---|---|---|
| **Access token** | 1 hora | `jwt.expirationMs` |
| **Refresh token** | 7 días | `jwt.refreshTokenExpirationMs` |

**Rotación**: cada `POST /auth/refresh` revoca el refresh viejo y emite un par nuevo. Esto permite detección de robo de refresh tokens (si alguien intenta usar un refresh ya rotado, ambas sesiones se invalidan).

**Logout**: revoca el refresh token en BD y publica el access token en blacklist Redis con TTL = lifetime restante. Una vez expira el JWT natural, Redis lo borra solo.

---

## 3. Formato común de respuestas

**Todas** las respuestas REST usan `ApiResponse<T>`:

### Success

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { /* T */ },
  "status": 200,
  "timestamp": "2026-04-25T21:30:45.123"
}
```

### Created (201)

```json
{
  "success": true,
  "message": "Recurso creado exitosamente",
  "data": { /* T */ },
  "status": 201,
  "timestamp": "2026-04-25T21:30:45.123"
}
```

### Lista paginada (cuando aplique)

```json
{
  "success": true,
  "data": {
    "content": [ /* items */ ],
    "page": 0,
    "size": 20,
    "totalElements": 145,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "status": 200
}
```

---

## 4. Errores estándar

| HTTP | Causa | Excepción Java |
|---|---|---|
| **400** | Validación de DTO falló | `MethodArgumentNotValidException` |
| **400** | Regla de negocio rota | `BusinessException` |
| **401** | Credenciales inválidas o expiradas | `InvalidCredentialsException` |
| **401** | JWT inválido / blacklisted | (Gateway rechaza) |
| **403** | Refresh token inválido | `TokenRefreshException` |
| **403** | Falta rol/permiso | Spring Security |
| **404** | Recurso no existe | `ResourceNotFoundException` |
| **409** | Code duplicado | `DuplicateResourceException` |
| **429** | Rate limit excedido | (Gateway / Redis) |
| **500** | Error inesperado | resto |

### Formato de error

```json
{
  "success": false,
  "message": "Recurso no encontrado: Rol con Id 'abc-123'",
  "status": 404,
  "timestamp": "2026-04-25T21:30:45.123"
}
```

### Error de validación (400)

```json
{
  "success": false,
  "message": "Error de validación",
  "data": {
    "username": "must not be blank",
    "email": "must be a well-formed email address"
  },
  "status": 400
}
```

---

## 5. Endpoints — Auth Service

> **Base URL**: `http://localhost:8080` (vía gateway)
> **Todos los endpoints requieren `Authorization: Bearer <token>` excepto los marcados como 🔓 (públicos).**

### 5.1 Autenticación

#### 🔓 `POST /auth/login`

Login con username o email.

**Request**:
```json
{
  "usernameOrEmail": "admin",
  "password": "Admin123!"
}
```

**Response 200**:
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "f4a3b2c1d5e6...64hex...",
      "tokenType": "Bearer",
      "expiresInSeconds": 3600
    },
    "user": {
      "id": "e7bfb3c5-9fc6-4f79-b249-cacd9a9cb151",
      "username": "admin",
      "email": "admin@saas.local",
      "firstName": "Administrador",
      "lastName": "Sistema",
      "fullName": "Administrador Sistema",
      "profilePhoto": null,
      "theme": "light",
      "languageCode": "es-CO",
      "lastLoginAt": "2026-04-25T21:21:17.580",
      "enabled": true,
      "visible": true,
      "roleCodes": ["ADMIN"],
      "createdDate": "2026-04-25T21:21:17.689"
    }
  },
  "status": 200
}
```

**Errores**: 401 (credenciales inválidas), 401 (cuenta deshabilitada), 429 (rate limit).

---

#### 🔓 `POST /auth/refresh`

Rota el par de tokens. Revoca el refresh viejo y emite uno nuevo.

**Request**:
```json
{ "refreshToken": "f4a3b2c1d5e6..." }
```

**Response 200**:
```json
{
  "success": true,
  "message": "Token refrescado",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "9b8c7d6e5f4a...",
    "tokenType": "Bearer",
    "expiresInSeconds": 3600
  },
  "status": 200
}
```

**Errores**: 403 (refresh no encontrado / expirado / revocado).

---

#### `POST /auth/logout`

Revoca el refresh token + blacklistea el access token en Redis.

**Headers**: `Authorization: Bearer <accessToken>`

**Request** (opcional — si lo envías, también revoca el refresh):
```json
{ "refreshToken": "f4a3b2c1d5e6..." }
```

**Response 200**:
```json
{ "success": true, "message": "Logout exitoso", "status": 200 }
```

---

#### `POST /auth/logout-all`

Cierra **todas** las sesiones del usuario actual (revoca todos sus refresh tokens). Útil cuando se sospecha compromiso de cuenta.

**Response 200**:
```json
{ "success": true, "message": "Sesiones cerradas", "status": 200 }
```

---

### 5.2 Usuarios

#### `GET /users/me`

Perfil del usuario autenticado, con sus roles efectivos.

**Response 200**: ver `UserResponse` arriba.

---

#### `POST /users/me/change-password`

Cambia el password del usuario autenticado.

**Request**:
```json
{
  "currentPassword": "Admin123!",
  "newPassword": "NuevoPassword456!"
}
```

**Response 200**:
```json
{ "success": true, "message": "Password actualizado", "status": 200 }
```

**Errores**: 401 (currentPassword incorrecto), 400 (newPassword igual al actual o < 8 chars).

---

#### `GET /users` 🔒 ADMIN

Lista todos los usuarios.

**Response 200**: `ApiResponse<List<UserResponse>>`

---

#### `GET /users/{id}` 🔒 ADMIN

Usuario por Id (UUID), incluye sus roles.

---

#### `POST /users` 🔒 ADMIN

Crea un usuario y opcionalmente le asigna roles.

**Request**:
```json
{
  "username": "jperez",
  "email": "jperez@empresa.com",
  "password": "Inicial123!",
  "firstName": "Juan",
  "lastName": "Pérez",
  "profilePhoto": "https://cdn.example.com/photos/jperez.png",
  "theme": "dark",
  "languageCode": "es-CO",
  "roleIds": ["11111111-0000-0000-0000-000000000002"]
}
```

**Response 201**: `ApiResponse<UserResponse>`

**Errores**: 409 (username/email ya existe), 404 (algún roleId no existe).

---

#### `PUT /users/{id}` 🔒 ADMIN

Actualiza un usuario. **Patch parcial**: campos `null` se ignoran (no sobrescriben).

**Request** (todos opcionales):
```json
{
  "username": "jperez2",
  "email": "jperez@nuevo.com",
  "firstName": "Juan Carlos",
  "theme": "light"
}
```

> El password NO se cambia por aquí — usa `/users/me/change-password` o un endpoint admin de reset (no implementado en la línea base).

---

#### `POST /users/{id}/roles` 🔒 ADMIN

**Reemplaza** el set completo de roles del usuario (idempotente).

**Request**:
```json
{
  "roleIds": [
    "11111111-0000-0000-0000-000000000001",
    "11111111-0000-0000-0000-000000000002"
  ]
}
```

**Response 200**: `{ "success": true, "message": "Roles asignados" }`

---

#### `DELETE /users/{id}` 🔒 ADMIN

Soft-delete (`Enabled = Visible = false`). El usuario sigue existiendo en BD.

---

## 6. Endpoints — System Service

> Igualmente vía gateway (`http://localhost:8080`). Todos requieren JWT.

### 6.1 Roles

| Método | Path | Rol | Descripción |
|---|---|---|---|
| GET | `/roles` | (auth) | Lista todos los roles |
| GET | `/roles/{id}` | (auth) | Por UUID |
| GET | `/roles/code/{code}` | (auth) | Por código (ej. `ADMIN`) |
| POST | `/roles` | ADMIN | Crear rol |
| PUT | `/roles/{id}` | ADMIN | Actualizar |
| DELETE | `/roles/{id}` | ADMIN | Soft-delete |

#### Sub-recurso: permisos del rol

| Método | Path | Rol | Descripción |
|---|---|---|---|
| GET | `/roles/{id}/permissions` | (auth) | Permisos asignados al rol |
| PUT | `/roles/{id}/permissions` | ADMIN | **Reemplaza** el set de permisos |

**`POST /roles` Request**:
```json
{
  "code": "EDITOR",
  "name": "Editor de contenido",
  "description": "Puede crear y editar pero no eliminar"
}
```

**`PUT /roles/{id}/permissions` Request**:
```json
{
  "ids": [
    "22222222-0000-0000-0000-000000000001",
    "22222222-0000-0000-0000-000000000002",
    "22222222-0000-0000-0000-000000000003"
  ]
}
```

---

### 6.2 Permisos

| Método | Path | Rol |
|---|---|---|
| GET | `/permissions` | (auth) |
| GET | `/permissions/{id}` | (auth) |
| GET | `/permissions/code/{code}` | (auth) |
| POST | `/permissions` | ADMIN |
| PUT | `/permissions/{id}` | ADMIN |
| DELETE | `/permissions/{id}` | ADMIN |

**Request body** (POST/PUT):
```json
{
  "code": "EXPORT",
  "name": "Exportar",
  "description": "Permite exportar información"
}
```

> Los permisos seed son: `VIEW`, `CREATE`, `EDIT`, `DELETE`, `EXPORT`, `IMPORT`.

---

### 6.3 Menús

#### Listado

| Método | Path | Rol | Descripción |
|---|---|---|---|
| GET | `/menus` | ADMIN | Lista plana de todos los menús |
| GET | `/menus/tree` | ADMIN | Árbol jerárquico completo |
| GET | `/menus/me` | (auth) | **Árbol filtrado para el usuario actual** |
| GET | `/menus/{id}` | (auth) | Por Id |

#### CRUD

| Método | Path | Rol |
|---|---|---|
| POST | `/menus` | ADMIN |
| PUT | `/menus/{id}` | ADMIN |
| DELETE | `/menus/{id}` | ADMIN |

#### Asignar roles

| Método | Path | Rol |
|---|---|---|
| GET | `/menus/{id}/roles` | ADMIN |
| PUT | `/menus/{id}/roles` | ADMIN |

**`POST /menus` Request**:
```json
{
  "code": "DASHBOARD_VENTAS",
  "name": "Ventas",
  "icon": "shopping-cart",
  "route": "/dashboard/ventas",
  "parentId": "77777777-0000-0000-0000-000000000001",
  "displayOrder": 2
}
```
- `parentId = null` → menú raíz (sección principal)
- `parentId = <UUID>` → submenú

**`GET /menus/me` Response 200** (ejemplo):
```json
{
  "success": true,
  "data": [
    {
      "id": "77777777-0000-0000-0000-000000000001",
      "code": "DASHBOARD",
      "name": "Dashboard",
      "icon": "dashboard",
      "route": "/dashboard",
      "parentId": null,
      "displayOrder": 1,
      "enabled": true,
      "visible": true,
      "children": []
    },
    {
      "id": "77777777-0000-0000-0000-000000000002",
      "code": "CONFIG",
      "name": "Configuracion",
      "icon": "settings",
      "route": null,
      "parentId": null,
      "displayOrder": 9,
      "enabled": true,
      "visible": true,
      "children": [
        {
          "id": "77777777-0000-0000-0000-000000000010",
          "code": "CONFIG_USERS",
          "name": "Usuarios",
          "icon": "users",
          "route": "/config/users",
          "parentId": "77777777-0000-0000-0000-000000000002",
          "displayOrder": 1,
          "children": []
        }
      ]
    }
  ],
  "status": 200
}
```

---

### 6.4 Listas del sistema

Catálogos configurables (tipos de documento, géneros, estados, etc.) con sus items.

#### Listas

| Método | Path | Rol |
|---|---|---|
| GET | `/system-lists` | (auth) |
| GET | `/system-lists/{id}` | (auth) |
| GET | `/system-lists/code/{code}` | (auth) |
| POST | `/system-lists` | ADMIN |
| PUT | `/system-lists/{id}` | ADMIN |
| DELETE | `/system-lists/{id}` | ADMIN |

#### Items dentro de una lista

| Método | Path | Rol | Descripción |
|---|---|---|---|
| GET | `/system-lists/{listId}/items` | (auth) | Por Id de lista |
| GET | `/system-lists/code/{listCode}/items` | (auth) | **⭐ Lookup típico desde frontend** |
| GET | `/system-lists/code/{listCode}/items/{itemCode}` | (auth) | Item específico por códigos |
| POST | `/system-lists/{listId}/items` | ADMIN | Crear item |
| PUT | `/system-lists/{listId}/items/{itemId}` | ADMIN | Actualizar |
| DELETE | `/system-lists/{listId}/items/{itemId}` | ADMIN | Soft-delete |

**Ejemplo típico desde frontend** — obtener tipos de documento para llenar un dropdown:

```
GET /system-lists/code/TIPOS_DOCUMENTO/items
```

```json
{
  "success": true,
  "data": [
    { "id": "55555555-0000-0000-0000-000000000001", "listId": "...", "code": "CC", "name": "Cedula de Ciudadania", "value": "CC", "displayOrder": 1, ... },
    { "code": "TI", "name": "Tarjeta de Identidad", "value": "TI", "displayOrder": 2, ... },
    { "code": "CE", "name": "Cedula de Extranjeria", "value": "CE", "displayOrder": 3, ... },
    { "code": "PA", "name": "Pasaporte", "value": "PA", "displayOrder": 4, ... },
    { "code": "NIT", "name": "NIT", "value": "NIT", "displayOrder": 5, ... }
  ]
}
```

**`POST /system-lists/{listId}/items` Request**:
```json
{
  "code": "RC",
  "name": "Registro Civil",
  "value": "RC",
  "displayOrder": 6
}
```

> **Nota**: el `code` del item es único **dentro de su lista** (no globalmente). Puedes tener `code=ACT` en `ESTADOS_REGISTRO` y otra cosa en otra lista.

---

### 6.5 Constantes

Valores de configuración global. **Todo se guarda como STRING**; el consumidor decide cómo interpretarlo.

| Método | Path | Rol |
|---|---|---|
| GET | `/constants` | (auth) |
| GET | `/constants/{id}` | (auth) |
| GET | `/constants/code/{code}` | (auth) — **lookup típico** |
| POST | `/constants` | ADMIN |
| PUT | `/constants/{id}` | ADMIN |
| DELETE | `/constants/{id}` | ADMIN |

**Ejemplo**:
```
GET /constants/code/MAYORIA_EDAD
```
```json
{
  "success": true,
  "data": {
    "id": "66666666-0000-0000-0000-000000000001",
    "code": "MAYORIA_EDAD",
    "name": "Mayoria de edad",
    "value": "18",
    "description": "Edad minima para ser mayor de edad",
    "enabled": true,
    "visible": true,
    "createdDate": "...",
    "auditDate": "..."
  }
}
```

**Constantes seed**:
| Code | Value | Descripción |
|---|---|---|
| `MAYORIA_EDAD` | `18` | Edad mínima para ser mayor de edad |
| `MAX_LOGIN_ATTEMPTS` | `5` | Intentos antes de bloquear cuenta |
| `SESION_TIMEOUT_MIN` | `60` | Timeout de sesión (minutos) |
| `PROFILE_PHOTO_MAX_KB` | `512` | Tamaño máx. foto de perfil |

---

## 7. Endpoints — Gateway / Info

| Método | Path | Servicio | Descripción |
|---|---|---|---|
| GET | `/api/info` | gateway | Info del servicio (nombre, profile, java, os) |
| GET | `/api/info` | auth (vía `/auth/api/info` no expuesto, info general por gateway) | Para diagnóstico |
| GET | `/actuator/health` | todos | Health check |
| GET | `/actuator/info` | todos | Build info |
| GET | `/actuator/gateway/routes` | gateway | Rutas registradas (útil para debug) |

---

## 8. Endpoints internos (S2S)

> 🚫 **NO se exponen vía gateway**. Solo accesibles dentro de la red interna (Eureka/Feign). El gateway no tiene rutas hacia `/internal/**`.

Los consume **auth-service** desde su `SystemServiceClient` (Feign):

| Método | Path | Para qué |
|---|---|---|
| POST | `/internal/roles/codes` | Resolver `Set<UUID>` → `Map<UUID, String>` (códigos de rol). Usado en login para construir el JWT |
| GET | `/internal/roles/{roleId}/permissions/codes` | `Set<String>` con los codes de permisos del rol |

> Si necesitas exponer estos endpoints a un servicio externo (no recomendado), agrégalos al `RouteConfig.java` del gateway con auth.

---

## 9. Manejo de ambientes (profiles)

### Arquitectura de profiles

```
saas-config-repo/
├── application.properties              ← global, comparte por todos
├── application-local.properties        ← desarrollo local (default)
├── application-docker.properties       ← stack completo en Docker
├── application-dev.properties          ← servidor remoto de desarrollo
├── application-prod.properties         ← producción (todo via env vars)
├── auth-service.properties             ← config específica de auth (todos los ambientes)
├── system-service.properties           ← idem
├── gateway-service.properties          ← idem
└── discovery-service.properties        ← idem
```

**Cómo se combinan**: cuando un servicio arranca con profile `X`, el config-server le sirve la unión de:

1. `application.properties` (global)
2. `application-X.properties` (ambiente)
3. `<service-name>.properties` (servicio)
4. `<service-name>-X.properties` (servicio + ambiente, si existe)

**Lo de abajo gana sobre lo de arriba** (más específico → más prioritario).

### Diferencias entre profiles

| Profile | DB | Eureka | Redis | JWT secret | Logs |
|---|---|---|---|---|---|
| **local** | `localhost:3306` | `localhost:8761` | `localhost:6379` | hardcoded | DEBUG |
| **docker** | `mysql:3306` | `discovery-service:8761` | `redis:6379` | hardcoded | INFO |
| **dev** | `${DEV_DB_HOST}` (default `dev-server`) | `${DEV_EUREKA_HOST}` | `${DEV_REDIS_HOST}` | env var `JWT_SECRET` | INFO |
| **prod** | `${DB_URL}` (env var) | `${EUREKA_URL}` | `${REDIS_HOST}` | env var `JWT_SECRET` | WARN |

### Cómo se activa cada profile

| Modo | Quién lo setea | Cómo |
|---|---|---|
| **IntelliJ** | Implícito | `spring.profiles.default=local` en cada `application.properties` local |
| **Docker** | `docker-compose.yml` | `SPRING_PROFILES_ACTIVE: docker` |
| **CLI** | Variable de entorno | `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` |
| **CLI** | Argumento JVM | `java -Dspring.profiles.active=prod -jar app.jar` |

---

## 10. Cambiar de modo: IntelliJ ↔ Docker

### A) Modo IntelliJ (desarrollo día a día)

**En Docker corre solo la infra** (`mysql` + `redis`). Los servicios Java corren en IntelliJ con profile `local` (automático).

```bash
# 1. Asegurar que MySQL/Redis local NO están corriendo (puerto 3306, 6379 libres)
# 2. Levantar solo infra
docker compose up -d mysql redis
```

**En IntelliJ**, abre 5 Run Configurations en este orden:

| Orden | App | Espera... |
|---|---|---|
| 1 | `ConfigServerApplication` | "Started ConfigServerApplication" |
| 2 | `DiscoveryServiceApplication` | "Started DiscoveryServiceApplication" |
| 3 | `AuthServiceApplication` | "Admin user creado" |
| 4 | `SystemServiceApplication` | "Started" |
| 5 | `GatewayServiceApplication` | "Started" |

**Sin tocar nada más** en IntelliJ. El profile `local` se activa solo gracias al `spring.profiles.default=local` que vive en cada `src/main/resources/application.properties`.

> **Tip Compound Run Config**: Run → Edit Configurations → "+" → Compound. Añade los 5 con un delay (ej. 8s entre cada uno).

### B) Modo Docker (frontend / demo / producción dev)

```bash
docker compose down -v               # limpia volúmenes (BD desde cero)
docker compose build --no-cache      # solo si tocaste código Java
docker compose up -d
```

Los profiles los setea `docker-compose.yml`:
- `auth/system/gateway` → `docker`
- `discovery-service` → sin profile (usa base)
- `config-server` → `native`

### C) Mezclar (no recomendado)

Si por alguna razón necesitas correr **algunos** servicios en Docker y otros en IntelliJ:

- En IntelliJ los servicios usan `localhost`, así que registran con Eureka como `localhost:8082` etc. Funciona si TODO lo demás también está en el mismo host.
- En Docker los contenedores no pueden alcanzar `localhost` del host (excepto via `host.docker.internal`).
- **Recomendación**: usa A o B puros. Mezclar trae bugs raros.

### Cómo saber qué modo está activo

En el log de cualquier servicio al arrancar:

```
The following 1 profile is active: "local"   ← IntelliJ
The following 1 profile is active: "docker"  ← Docker compose
```

O por endpoint:

```bash
curl http://localhost:8082/api/info
# {"data": { "service": "auth-service", "environment": "local", ... }}
```

---

## 11. Cambiar de ambiente: local → dev → prod

### Local (default)

No haces nada. Por defecto cuando corres en IntelliJ, profile=`local`.

### Dev (servidor remoto)

Cuando el servidor remoto esté arriba, en IntelliJ Run Configuration de cada servicio:

**Edit Configuration → Spring Boot → Active profiles**: `dev`

Y configura las env vars del ambiente (Run Config → Environment variables):

```
DEV_DB_HOST=mi-servidor-dev.empresa.com
DEV_DB_PORT=3306
DEV_DB_USER=saasuser
DEV_DB_PASSWORD=********
DEV_EUREKA_HOST=mi-servidor-dev.empresa.com
DEV_EUREKA_PORT=8761
DEV_REDIS_HOST=mi-servidor-dev.empresa.com
DEV_REDIS_PORT=6379
```

> Si no las setees, los defaults son `dev-server` (placeholders) y todo va a fallar. Edita `application-dev.properties` si quieres cambiar los defaults.

### Prod

`prod` espera **todo** vía env vars:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://prod-db.empresa.com:3306/saas_db?...'
export DB_USERNAME=saasapp
export DB_PASSWORD='<secret>'
export EUREKA_URL='http://eureka.empresa.com:8761/eureka/'
export REDIS_HOST=redis.empresa.com
export REDIS_PORT=6379
export REDIS_PASSWORD='<secret>'
export JWT_SECRET='<secret-min-32-chars>'
export JWT_EXPIRATION_MS=900000              # 15 min
export JWT_REFRESH_EXPIRATION_MS=604800000   # 7 días

java -jar auth-service.jar
```

### Crear un nuevo ambiente (ej. `qa`, `test`, `staging`)

1. Crea `saas-config-repo/application-qa.properties` (copia de `application-dev.properties` y ajusta hosts).
2. Activa con `SPRING_PROFILES_ACTIVE=qa`.

No hay que recompilar nada — el config-server lee los archivos del filesystem en cada request.

---

## 12. Rate limiting

Aplicado en el gateway, usando Redis como bucket store (Spring Cloud Gateway `RedisRateLimiter`).

| Ruta | Bucket | Replenish | Burst | Key |
|---|---|---|---|---|
| `POST /auth/login` | login | 2 req/seg | 5 | **IP** del cliente |
| Resto de auth (`/auth/*`, `/users/*`) | default | 20 req/seg | 40 | userId si autenticado, IP si no |
| Sistema (`/roles/*`, `/menus/*`, etc.) | default | 20 req/seg | 40 | userId si autenticado, IP si no |

**Cómo se detecta**: si excedes, el gateway responde **429 Too Many Requests** con headers `X-RateLimit-Remaining`, `X-RateLimit-Burst-Capacity`, `X-RateLimit-Replenish-Rate`.

**Por qué login usa IP, no usuario**: si un atacante prueba passwords contra `victima@empresa.com`, no queremos que su rate-limit consuma el bucket de la víctima. Usar IP cierra esa puerta.

**Tunear en runtime**: edita `gateway-service.properties` y reinicia el gateway:

```properties
saas.gateway.rate-limit.default.replenish-rate=20
saas.gateway.rate-limit.default.burst-capacity=40
saas.gateway.rate-limit.login.replenish-rate=2
saas.gateway.rate-limit.login.burst-capacity=5
```

---

## 13. Datos seed (cuenta admin, roles, etc.)

### Cuenta admin

Creada por `DataInitializer` al arrancar auth-service (idempotente):

```
Username:  admin
Email:     admin@saas.local
Password:  Admin123!
Rol:       ADMIN
```

> ⚠️ **Cambia el password** tras el primer login en cualquier ambiente que no sea local.

Configurable en `auth-service.properties`:
```properties
saas.bootstrap.admin.username=admin
saas.bootstrap.admin.email=admin@saas.local
saas.bootstrap.admin.password=Admin123!
saas.bootstrap.admin.first-name=Administrador
saas.bootstrap.admin.last-name=Sistema
```

### Roles seed (Flyway V2)

| Code | Name | Permisos |
|---|---|---|
| `ADMIN` | Administrador | Todos (VIEW, CREATE, EDIT, DELETE, EXPORT, IMPORT) |
| `USER` | Usuario | VIEW, CREATE, EDIT |
| `GUEST` | Invitado | VIEW |

### Permisos seed

`VIEW`, `CREATE`, `EDIT`, `DELETE`, `EXPORT`, `IMPORT`

### Listas del sistema seed

| Code | Items |
|---|---|
| `TIPOS_DOCUMENTO` | CC, TI, CE, PA, NIT |
| `ESTADOS_REGISTRO` | ACT, INA, PEN, BLO |
| `GENEROS` | M, F, O |

### Constantes seed

`MAYORIA_EDAD=18`, `MAX_LOGIN_ATTEMPTS=5`, `SESION_TIMEOUT_MIN=60`, `PROFILE_PHOTO_MAX_KB=512`

### Menús seed

Dashboard + Configuración (con sub-menús: Usuarios, Roles, Menús, Listas, Constantes).

---

## Apéndice A — Smoke test rápido

```bash
# 1) Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}' \
  | jq -r '.data.tokens.accessToken')
echo "Token: $TOKEN"

# 2) Mis menús
curl -s http://localhost:8080/menus/me \
  -H "Authorization: Bearer $TOKEN" | jq

# 3) Catálogo de tipos de documento
curl -s http://localhost:8080/system-lists/code/TIPOS_DOCUMENTO/items \
  -H "Authorization: Bearer $TOKEN" | jq

# 4) Una constante
curl -s http://localhost:8080/constants/code/MAYORIA_EDAD \
  -H "Authorization: Bearer $TOKEN" | jq

# 5) Logout
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<el refresh del paso 1>"}'
```

PowerShell equivalente:

```powershell
$resp = Invoke-RestMethod -Uri http://localhost:8080/auth/login -Method POST `
  -ContentType "application/json" `
  -Body '{"usernameOrEmail":"admin","password":"Admin123!"}'
$token = $resp.data.tokens.accessToken
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri http://localhost:8080/menus/me -Headers $headers | ConvertTo-Json -Depth 6
Invoke-RestMethod -Uri http://localhost:8080/system-lists/code/TIPOS_DOCUMENTO/items -Headers $headers
Invoke-RestMethod -Uri http://localhost:8080/constants/code/MAYORIA_EDAD -Headers $headers
```

---

## Apéndice B — Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Connection refused: localhost:3306` | MySQL no está corriendo | `docker compose up -d mysql` |
| `Connection refused: localhost:6379` | Redis no está corriendo | `docker compose up -d redis` |
| `Connect to localhost:8888 failed` | Config-server no arrancó | Arrancarlo primero en IntelliJ |
| Auth-service: `Schema-validation: missing table [auth_xxx]` | Imagen Docker vieja | `docker compose down -v && docker compose build --no-cache && docker compose up -d` |
| Gateway: 503 al hacer cualquier request | Servicio destino no registrado en Eureka | Esperar ~30s tras arrancar el servicio (Eureka cache) o reiniciar |
| Gateway: 429 inmediato en login | Rate limit muy bajo | Reduce `saas.gateway.rate-limit.login.*` o espera el replenish |
| Login retorna `roleCodes: []` | Feign no pudo resolver, system-service caído | Verifica que system-service esté UP en Eureka |
| `Schema-validation: missing column [Xxx]` | Entidad nueva sin migración Flyway | Crear `V3__add_xxx.sql` en `auth-service/src/main/resources/db/migration/` |
| `port already in use: 3306` | Tu MySQL local sigue corriendo | Apagar MySQL local o cambiar puerto del Docker MySQL a 3307 |

---

**Última actualización**: 2026-04-25 — refactor a línea base con UUID + auditoría unificada.
