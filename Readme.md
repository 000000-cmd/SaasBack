# SaaS Platform — Documentación Completa

> Plataforma de microservicios multi-negocio (gestor multi-business). Spring Boot 3.5.9, Java 21, Spring Cloud 2025.0.1.
>
> **Stack**: MySQL 8.4 · Redis 7.4 · Kafka 7.8 (cluster KRaft) · Elasticsearch 8.17 · Eureka · Spring Cloud Config · Spring Cloud Gateway.

---

## 📚 Tabla de contenidos

1. [Visión general](#1-visión-general)
2. [Arquitectura](#2-arquitectura)
3. [Servicios y puertos](#3-servicios-y-puertos)
4. [Stack y patrones](#4-stack-y-patrones)
5. [Inicio rápido](#5-inicio-rápido)
6. [Variables de entorno](#6-variables-de-entorno)
7. [Estructura del proyecto](#7-estructura-del-proyecto)
8. [Flujo de datos: BD → Outbox → Kafka → Elasticsearch](#8-flujo-de-datos-bd--outbox--kafka--elasticsearch)
9. [Autenticación y JWT](#9-autenticación-y-jwt)
10. [Formato de respuestas](#10-formato-de-respuestas)
11. [Errores estándar](#11-errores-estándar)
12. [Endpoints — Auth](#12-endpoints--auth)
13. [Endpoints — System](#13-endpoints--system)
14. [Endpoints — Search (Elasticsearch)](#14-endpoints--search-elasticsearch)
15. [Endpoints internos S2S](#15-endpoints-internos-s2s)
16. [Reindex masivo](#16-reindex-masivo)
17. [Manejo de ambientes (profiles)](#17-manejo-de-ambientes-profiles)
18. [Cambiar entre IntelliJ y Docker](#18-cambiar-entre-intellij-y-docker)
19. [Rate limiting](#19-rate-limiting)
20. [CORS](#20-cors)
21. [Datos seed](#21-datos-seed)
22. [Migraciones Flyway](#22-migraciones-flyway)
23. [Agregar entidad nueva indexable](#23-agregar-entidad-nueva-indexable)
24. [Agregar microservicio nuevo](#24-agregar-microservicio-nuevo)
25. [Comandos útiles](#25-comandos-útiles)
26. [Troubleshooting](#26-troubleshooting)
27. [Producción y hardening](#27-producción-y-hardening)

---

## 1. Visión general

Plataforma SaaS de gestión multi-negocio (fashion, peluquería, etc.) construida con arquitectura de microservicios y CQRS para casos de búsqueda/agregación intensiva.

**Pilares**:

- **Microservicios** desacoplados con Spring Cloud (Eureka + Config Server + Gateway).
- **Multi-tenancy** desde el inicio (campo `businessId` en eventos y documentos).
- **CQRS asíncrono**: MySQL es la fuente de verdad (writes), Elasticsearch es el read-model optimizado (búsquedas + agregaciones).
- **Outbox Pattern + Kafka** para garantizar consistencia eventual entre BD y read-model sin "dual-write problem".
- **Cluster productivo**: Kafka 3 brokers + ES 1 nodo (dev) / 3 nodos (prod).

---

## 2. Arquitectura

```
                            ┌─────────────────────────────────────┐
   Front (Angular) ───────▶ │     GATEWAY (8080)                  │
                            │  JWT validation + Routing + CORS    │
                            └────────┬────────────────────────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                      ▼
    ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐
    │  AUTH SERVICE     │  │ SYSTEM SERVICE    │  │  SEARCH SERVICE   │
    │       (8082)      │  │       (8083)      │  │      (8085)       │
    │  Login, JWT,      │  │  Roles, perms,    │  │  Read model ES    │
    │  Users, Outbox    │  │  Menus, Outbox    │  │  Consumer Kafka   │
    └────────┬──────────┘  └────────┬──────────┘  └─────────┬─────────┘
             │                      │                       │
             └──────────────┬───────┴───────────────────────┤
                            ▼                               ▼
                   ┌────────────────┐           ┌──────────────────────┐
                   │ MySQL (3306)   │           │ Elasticsearch (9200) │
                   │  saas_db       │           │  índices: users,     │
                   │  + outbox_event│           │  roles (con alias)   │
                   └────────┬───────┘           └──────────▲───────────┘
                            │                              │
                            │  Outbox Relay (cada 2s)      │ index docs
                            ▼                              │
                   ┌──────────────────────────────────────┐│
                   │   KAFKA CLUSTER (KRaft)              ││
                   │   3 brokers (9094, 9095, 9096)       │┤
                   │   topic: domain.events               ││
                   └──────────────────────────────────────┘│
                                                           │
                                                  ┌────────┴────────┐
                                                  │ search consumer │
                                                  │  + dedup Redis  │
                                                  └─────────────────┘

  Infraestructura compartida:
  ┌─────────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
  │ Eureka (8761)       │  │ Config (8888)    │  │ Redis (6379)        │
  │ Service Discovery   │  │ Centraliza props │  │ Cache + JWT         │
  └─────────────────────┘  └──────────────────┘  │ blacklist + rate-l  │
                                                 │ + dedup eventos     │
                                                 └─────────────────────┘

  UIs (dev): Kibana (5601) · Kafdrop (9000) · Eureka (8761)
```

### Por qué este diseño

- **Auth y System** usan MySQL para CRUD transaccional. **Search** usa ES para queries pesadas.
- **Kafka desacopla** los productores (auth, system) de los consumidores (search, futuros: notifications, audit). Si Kafka cae, los eventos se acumulan en `outbox_event`; cuando vuelve, el `OutboxRelay` los publica.
- **El cluster Kafka de 3 brokers** garantiza tolerancia a fallos en producción (replication factor 3).
- **El cluster ES** se reduce a 1 nodo en dev (RAM limitada) y 3 en producción.

---

## 3. Servicios y puertos

| Servicio | Puerto | Rol | Stack |
|---|---|---|---|
| **gateway-service** | 8080 | Punto de entrada. Valida JWT, routing, rate-limit, CORS | Spring Cloud Gateway (reactivo) |
| **auth-service** | 8082 | Login, refresh, logout, usuarios. **Líder de Flyway** | Spring Boot + JPA + Outbox |
| **system-service** | 8083 | Roles, permisos, menús, listas, constantes | Spring Boot + JPA + Outbox |
| **search-service** | 8085 | Read-model en ES. Consumer Kafka + endpoints `/search/*` | Spring Boot + Spring Data ES + Kafka |
| **discovery-service** | 8761 | Eureka — registro de servicios | Spring Cloud Eureka |
| **config-server** | 8888 | Sirve `saas-config-repo/*.properties` | Spring Cloud Config |
| **mysql** | 3306 | BD `saas_db` (compartida por auth + system) | MySQL 8.4 |
| **redis** | 6379 | Cache, JWT blacklist, rate-limit, dedup eventos | Redis 7.4 |
| **kafka-1/2/3** | 9094/9095/9096 | Cluster Kafka (KRaft) | Confluent Kafka 7.8 |
| **es-01** | 9200 | Elasticsearch (single-node en dev, cluster en prod) | Elasticsearch 8.17 |
| **kibana** | 5601 | UI para Elasticsearch (DevTools, índices, queries) | Kibana 8.17 |
| **kafdrop** | 9000 | UI para Kafka (topics, mensajes, consumer groups) | Kafdrop 4 |

### Convención de paths

| Path | Servicio destino | Por qué |
|---|---|---|
| `/auth/**` | auth-service | `server.servlet.context-path=/auth` |
| `/system/**` | system-service | `server.servlet.context-path=/system` |
| `/search/**` | search-service | `server.servlet.context-path=/search` |

> El frontend siempre llama al gateway (`localhost:8080`). El prefijo (`/auth`, `/system`, `/search`) determina el routing.

---

## 4. Stack y patrones

### Arquitectura de cada servicio (hexagonal simplificada)

```
src/main/java/com/saas/<servicio>/
├── domain/
│   ├── model/        — Dominio puro (sin anotaciones JPA)
│   └── port/
│       ├── in/       — Use case interfaces (IUserUseCase)
│       └── out/      — Repository interfaces (IUserRepositoryPort)
├── application/
│   ├── service/      — Implementación de use cases (UserService)
│   ├── mapper/       — DTO ↔ Domain (MapStruct)
│   └── dto/
│       ├── request/  — Inputs HTTP
│       ├── response/ — Outputs HTTP
│       └── event/    — Payloads para outbox/Kafka
└── infrastructure/
    ├── controller/   — REST endpoints
    ├── persistence/
    │   ├── entity/   — JPA entities (extends BaseEntity)
    │   ├── mapper/   — Domain ↔ JPA (MapStruct, extends IBaseMapper)
    │   ├── repository/ — Spring Data JPA interfaces
    │   └── adapter/  — Implementa port out (extends BaseJpaRepositoryAdapter)
    ├── client/       — Feign clients (S2S)
    ├── security/     — JWT, filters
    └── config/       — SecurityConfig, etc.
```

### Patrones implementados

| Patrón | Para qué | Dónde vive |
|---|---|---|
| **Hexagonal** | Aislar dominio de infra | Cada servicio |
| **Repository / Adapter** | Cambiar de BD sin tocar dominio | `*RepositoryAdapter` extiende `BaseJpaRepositoryAdapter` |
| **Outbox Pattern** | Consistencia BD ↔ Kafka sin dual-write | `OutboxPublisher` + `OutboxRelay` (saas-common) |
| **CQRS asíncrono** | Lecturas optimizadas en ES, writes en MySQL | search-service |
| **Event sourcing parcial** | Replay de eventos para reindex | `domain.events` topic |
| **Idempotency** | Dedup de eventos duplicados | Redis con `eventId` |

### Estandarización en `saas-common`

| Clase | Propósito |
|---|---|
| `BaseEntity` | Superclase JPA: id, enabled, visible, audit fields |
| `BaseDomain` | Espejo de BaseEntity sin anotaciones JPA |
| `IBaseMapper<D, E>` | Contrato MapStruct domain ↔ entity |
| `IGenericRepositoryPort<T, ID>` | CRUD base + paginación + count |
| `IGenericUseCase<T, ID>` | Idem para casos de uso |
| `GenericCrudService<T, ID>` | Implementación CRUD con hooks `onAfter*` |
| `BaseJpaRepositoryAdapter<D, E, ID>` | Adapter genérico con CRUD universal |
| `OutboxPublisher` / `Impl` | API para emitir eventos al outbox |
| `OutboxRelay` | Scheduled que publica outbox → Kafka |
| `KafkaProducerConfig` | Config del producer Kafka |
| `JwtAuthenticationFilter` | Filter compartido para validar JWT |
| `AuditorAwareImpl` | JPA auditing automático |

---

## 5. Inicio rápido

### Requisitos

- Docker 24+ y Docker Compose v2
- Java 21 (solo para desarrollo en IntelliJ)
- Maven 3.9+ (idem)
- 8GB RAM mínimo recomendado (4GB para infra + 2GB ES + 2GB para servicios)

### Modo Docker (todo en contenedores)

```bash
# 1. Configurar .env
cp .env.template .env       # si tienes template; si no, crea uno con las vars de la sección 6
nano .env                   # ajusta passwords

# 2. Build + arranque
docker compose up -d --build

# 3. Esperar ~2 minutos a que todo esté healthy
docker compose ps

# 4. Verificar
curl http://localhost:8080/actuator/health
curl http://localhost:9200/_cluster/health
```

### Modo IntelliJ (desarrollo día a día)

```bash
# 1. Solo la infra (Docker)
docker compose up -d mysql redis kafka-1 kafka-2 kafka-3 es-01

# 2. En IntelliJ, arrancar en este orden:
#    config-server → discovery-service → auth-service → system-service → search-service → gateway-service
```

> **Tip**: crea un Compound Run Config en IntelliJ con los 6 services y delays de ~8 segundos entre cada uno.

### Smoke test rápido

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}' \
  | jq -r '.data.tokens.accessToken')

# Ver mis menús
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/system/menus/me | jq

# Buscar roles en Elasticsearch
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/search/roles?q=admin" | jq
```

---

## 6. Variables de entorno

`.env` en la raíz del proyecto:

```bash
# ─── BASE DE DATOS ─────────────────────
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=saas_db

# ─── ZONA HORARIA ──────────────────────
TZ=America/Bogota

# ─── KAFKA ─────────────────────────────
KAFKA_CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk     # 22 chars base64

# ─── ELASTICSEARCH ─────────────────────
# Heap por nodo. Dev: 1g (single-node). Prod (3 nodos): 1g por nodo.
ES_JAVA_OPTS=-Xms1g -Xmx1g

# ─── KIBANA ────────────────────────────
KIBANA_ENCRYPTION_KEY=TpYtXFbKjzGCHkFGQcydvozoktclSkgZ
ELASTIC_PASSWORD=ChangeM3InProd
```

**En producción**:
- Cambia `MYSQL_ROOT_PASSWORD` por un secret real.
- Genera nuevo `KAFKA_CLUSTER_ID` con `kafka-storage random-uuid`.
- Sube `ES_JAVA_OPTS` a `-Xms2g -Xmx2g` o más.
- Cambia `KIBANA_ENCRYPTION_KEY` por una de 32 caracteres random.
- Activa `xpack.security.enabled=true` en ES.

---

## 7. Estructura del proyecto

```
saas-back/
├── auth-service/                    # Login, JWT, usuarios
├── system-service/                  # Roles, permisos, menús, listas, constantes
├── search-service/                  # Read model ES + consumer Kafka
├── gateway-service/                 # API Gateway
├── config-server/                   # Spring Cloud Config Server
├── discovery-service/               # Eureka
├── saas-common/                     # Librería compartida
│   └── src/main/java/com/saas/common/
│       ├── audit/                   # AuditorAwareImpl + JpaAuditingConfig
│       ├── controller/              # GlobalExceptionHandler, etc.
│       ├── dto/                     # ApiResponse
│       ├── events/                  # EventEnvelope, EventTypes
│       ├── exception/               # Exception classes
│       ├── kafka/                   # KafkaProducerConfig
│       ├── mapper/                  # IBaseMapper + BaseMapStructConfig
│       ├── model/                   # BaseDomain
│       ├── outbox/                  # OutboxEvent, Repository, Publisher, Relay
│       ├── persistence/             # BaseEntity, BaseJpaRepositoryAdapter
│       ├── port/                    # IGenericRepositoryPort, IGenericUseCase
│       ├── security/                # JwtAuthenticationFilter, IUserPrincipal
│       └── service/                 # GenericCrudService, CodeCrudService
├── saas-config-repo/                # Properties servidas por config-server
│   ├── application.properties               (global)
│   ├── application-local.properties         (IntelliJ)
│   ├── application-docker.properties        (Docker)
│   ├── application-dev.properties           (servidor remoto dev)
│   ├── application-prod.properties          (producción)
│   ├── auth-service.properties
│   ├── system-service.properties
│   ├── search-service.properties
│   ├── gateway-service.properties
│   └── discovery-service.properties
├── init-scripts/                    # SQL de inicialización (no usado actualmente)
├── docker-compose.yml               # Orquestación de todo el stack
├── Dockerfile                       # Multi-stage para construir TODOS los servicios
├── .env                             # Secrets locales (NO commit)
├── deploy.sh                        # Deploy en VPS
├── deploy-config.env                # Config de deploy
├── backup.sh                        # Backup de MySQL
├── check-services.sh                # Health checks
├── DEPLOY-README.md                 # Guía de deploy en VPS
└── Readme.md                        # ESTE archivo
```

---

## 8. Flujo de datos: BD → Outbox → Kafka → Elasticsearch

### El "happy path" cuando se crea un rol

```
1. POST /system/roles { code: "EDITOR", ... }
   ↓
2. Gateway valida JWT → rutea a system-service:8083
   ↓
3. RoleController → RoleService.create()
   ↓
4. @Transactional {
       INSERT en role (MySQL)         ← paso 4a
       INSERT en outbox_event (MySQL) ← paso 4b (mismo TX, atómico)
   }
   ↓
5. (Cada 2s) OutboxRelay.flush():
   - SELECT outbox_event WHERE Status='PENDING' FOR UPDATE SKIP LOCKED
   - Para cada uno: kafka.send("domain.events", key=businessId, json)
   - UPDATE Status='PUBLISHED'
   ↓
6. Kafka entrega el mensaje a search-service (consumer group: search-indexer)
   ↓
7. DomainEventListener.onMessage():
   - Deserializa EventEnvelope
   - Verifica dedup en Redis (eventId)
   - Busca handler que soporte type "role.created"
   ↓
8. RoleEventHandler.handle():
   - mapper.treeToValue(payload, RoleDocument.class)
   - ops.save(doc, IndexCoordinates.of("roles"))
   ↓
9. Elasticsearch indexa el documento (refresh ~1s)
   ↓
10. GET /search/roles?q=editor encuentra el doc
```

**Latencia total típica**: 2–3 segundos desde el `POST` hasta que aparece en `GET /search/roles`.

### Garantías

| Garantía | Cómo se logra |
|---|---|
| **Atomicidad BD ↔ Outbox** | Mismo `@Transactional` (commit/rollback juntos) |
| **No pérdida de eventos** | Outbox persiste en MySQL hasta confirmación de Kafka |
| **At-least-once en Kafka** | `acks=all` + `enable.idempotence=true` en producer |
| **No duplicados en ES** | Dedup por `eventId` en Redis (TTL 24h) |
| **Order preservado por entidad** | Key de Kafka = `businessId` (mismo aggregate → misma partición) |
| **Recuperación tras caída** | Reindex masivo via Feign desde MySQL |

### Estructura del `EventEnvelope`

```json
{
  "_v": 1,                                    // versión del envelope
  "eventId": "f47ac10b-...",                  // único, para dedup
  "type": "role.created",                     // routing al handler
  "version": 1,                               // versión del payload
  "businessId": null,                         // multi-tenancy (futuro)
  "aggregateId": "uuid-del-rol",
  "aggregateType": "role",
  "occurredAt": "2026-05-09T14:23:01Z",
  "producer": "system-service",
  "payload": { "code": "EDITOR", "name": "...", ... }
}
```

---

## 9. Autenticación y JWT

### Flujo de login

```
1. Cliente ──POST /auth/login──▶ Gateway ──▶ auth-service
2. Auth verifica password (BCrypt)
3. Auth resuelve roles via Feign ──▶ system-service (caché Caffeine 5min)
4. Auth genera JWT con userId + username + roles
5. Auth retorna { accessToken (1h), refreshToken (7d), user }
6. Siguientes requests: Authorization: Bearer <accessToken>
7. Gateway valida firma + verifica blacklist Redis
8. Gateway inyecta X-User-Id, X-User-Username, X-User-Roles
9. Servicio downstream confía y procesa
```

### Estructura del access token

```json
{
  "sub": "e7bfb3c5-9fc6-4f79-b249-cacd9a9cb151",   // userId UUID
  "username": "admin",
  "roles": ["ADMIN"],
  "iat": 1761953477,
  "exp": 1761957077                                  // iat + 1h
}
```

### TTL de tokens

| Token | Default | Property |
|---|---|---|
| Access | 1 hora | `jwt.expirationMs` |
| Refresh | 7 días | `jwt.refreshTokenExpirationMs` |

### Rotación

Cada `POST /auth/refresh`:
1. Revoca el refresh token usado.
2. Emite un nuevo par (access + refresh).

Esto detecta robo de refresh tokens: si alguien intenta usar uno ya rotado, ambas sesiones quedan invalidadas.

### Logout

- `POST /auth/logout` → revoca el refresh token + blacklist del access token en Redis (TTL = lifetime restante).
- `POST /auth/logout-all` → revoca **todos** los refresh tokens del usuario.

---

## 10. Formato de respuestas

Todas las respuestas REST usan `ApiResponse<T>`:

### Success

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { /* T */ },
  "status": 200,
  "timestamp": "2026-05-09T14:30:45.123"
}
```

### Created (201)

```json
{
  "success": true,
  "message": "Recurso creado exitosamente",
  "data": { /* T */ },
  "status": 201
}
```

### Lista paginada (Spring Page)

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
  }
}
```

### Lista paginada de Search Service

```json
{
  "success": true,
  "data": {
    "items": [ /* documentos ES */ ],
    "totalHits": 145,
    "page": 0,
    "size": 20,
    "totalPages": 8,
    "hasNext": true
  }
}
```

---

## 11. Errores estándar

| HTTP | Causa | Excepción |
|---|---|---|
| **400** | Validación de DTO | `MethodArgumentNotValidException` |
| **400** | Regla de negocio | `BusinessException` |
| **401** | Credenciales inválidas | `InvalidCredentialsException` |
| **401** | JWT inválido / blacklisted | (Gateway) |
| **403** | Refresh token inválido | `TokenRefreshException` |
| **403** | Falta rol/permiso | Spring Security |
| **404** | Recurso no existe | `ResourceNotFoundException` |
| **409** | Code/email/username duplicado | `DuplicateResourceException` |
| **429** | Rate limit excedido | (Gateway / Redis) |
| **500** | Error inesperado | resto |

### Formato de error

```json
{
  "success": false,
  "message": "Recurso no encontrado: Rol con Id 'abc-123'",
  "status": 404,
  "timestamp": "2026-05-09T14:30:45.123"
}
```

### Error de validación

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

## 12. Endpoints — Auth

> Base: `http://localhost:8080`. JWT obligatorio excepto endpoints marcados 🔓.

### Autenticación

| Método | Path | Body / Headers | Rol |
|---|---|---|---|
| 🔓 POST | `/auth/login` | `{ usernameOrEmail, password }` | público |
| 🔓 POST | `/auth/refresh` | `{ refreshToken }` | público |
| POST | `/auth/logout` | `{ refreshToken? }` + Bearer | (auth) |
| POST | `/auth/logout-all` | Bearer | (auth) |

### Usuarios

| Método | Path | Rol |
|---|---|---|
| GET | `/users/me` | (auth) |
| POST | `/users/me/change-password` | (auth) |
| GET | `/users` | ADMIN |
| GET | `/users/{id}` | ADMIN |
| POST | `/users` | ADMIN |
| PUT | `/users/{id}` | ADMIN |
| POST | `/users/{id}/roles` | ADMIN |
| DELETE | `/users/{id}` | ADMIN |

**`POST /users` Request**:
```json
{
  "username": "jperez",
  "email": "jperez@empresa.com",
  "password": "Inicial123!",
  "firstName": "Juan",
  "lastName": "Pérez",
  "theme": "dark",
  "languageCode": "es-CO",
  "roleIds": ["11111111-..."]
}
```

---

## 13. Endpoints — System

### Roles

| Método | Path | Rol |
|---|---|---|
| GET | `/system/roles` | (auth) |
| GET | `/system/roles/{id}` | (auth) |
| GET | `/system/roles/code/{code}` | (auth) |
| POST | `/system/roles` | ADMIN |
| PUT | `/system/roles/{id}` | ADMIN |
| DELETE | `/system/roles/{id}` | ADMIN |
| GET | `/system/roles/{id}/permissions` | (auth) |
| PUT | `/system/roles/{id}/permissions` | ADMIN |

### Permisos

| Método | Path | Rol |
|---|---|---|
| GET | `/system/permissions` | (auth) |
| POST | `/system/permissions` | ADMIN |
| PUT | `/system/permissions/{id}` | ADMIN |
| DELETE | `/system/permissions/{id}` | ADMIN |

### Menús

| Método | Path | Descripción |
|---|---|---|
| GET | `/system/menus` | Lista plana (ADMIN) |
| GET | `/system/menus/tree` | Árbol completo (ADMIN) |
| GET | `/system/menus/me` | Árbol filtrado para el user actual |
| POST | `/system/menus` | ADMIN |
| PUT | `/system/menus/{id}` | ADMIN |
| DELETE | `/system/menus/{id}` | ADMIN |
| PUT | `/system/menus/{id}/roles` | ADMIN |

### Listas del sistema

| Método | Path | Descripción |
|---|---|---|
| GET | `/system/system-lists` | Listar |
| GET | `/system/system-lists/code/{code}` | Por código |
| GET | `/system/system-lists/code/{listCode}/items` | **Lookup típico para dropdowns** |
| POST | `/system/system-lists` | ADMIN |
| POST | `/system/system-lists/{listId}/items` | ADMIN |

### Constantes

| Método | Path |
|---|---|
| GET | `/system/constants/code/{code}` |
| POST | `/system/constants` (ADMIN) |
| PUT | `/system/constants/{id}` (ADMIN) |
| DELETE | `/system/constants/{id}` (ADMIN) |

---

## 14. Endpoints — Search (Elasticsearch)

> Búsquedas optimizadas con full-text + filtros + paginación + ordenamiento.

### Convención de query params

| Param | Tipo | Descripción |
|---|---|---|
| `q` | string | Texto libre (full-text con fuzziness) |
| `page` | int | Default 0 |
| `size` | int | Default 20 |
| `sort` | string | Formato `campo,dir`. Ej: `fullName,asc` |
| `businessId` | UUID | Multi-tenancy. Opcional (futuro: del JWT) |

### Buscar usuarios

```
GET /search/users?q=&roleCodes=&enabled=&page=&size=&sort=
```

**Filtros**: `q` (full-text en username/email/fullName), `roleCodes` (lista, OR), `enabled` (boolean).

**Ejemplos**:
```bash
# Todos los usuarios
GET /search/users

# Búsqueda full-text
GET /search/users?q=juan

# Solo admins activos
GET /search/users?roleCodes=ADMIN&enabled=true

# Múltiples roles + sort
GET /search/users?roleCodes=ADMIN&roleCodes=USER&sort=fullName,asc

# Paginación
GET /search/users?page=2&size=50
```

### Buscar roles

```
GET /search/roles?q=&enabled=&page=&size=&sort=
```

**Filtros**: `q` (full-text en code/name/description), `enabled` (boolean).

**Ejemplos**:
```bash
GET /search/roles?q=admin
GET /search/roles?enabled=true&sort=code,asc
```

### Respuesta

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "businessId": null,
        "createdAt": "2026-05-09T14:00:00Z",
        "updatedAt": "2026-05-09T14:00:00Z",
        "docVersion": 1715266800000,
        "code": "ADMIN",
        "name": "Administrador",
        "description": "..."
      }
    ],
    "totalHits": 1,
    "page": 0,
    "size": 20,
    "totalPages": 1,
    "hasNext": false
  }
}
```

---

## 15. Endpoints internos S2S

> 🚫 **No expuestos vía gateway**. Solo accesibles dentro de la red interna (Eureka/Feign).

### Auth-service

| Método | Path | Consumido por |
|---|---|---|
| GET | `/auth/internal/users/all?page=&size=` | search-service (reindex) |
| GET | `/auth/internal/users/count` | search-service (reindex) |

### System-service

| Método | Path | Consumido por |
|---|---|---|
| POST | `/system/internal/roles/codes` | auth-service (login → resolver UUIDs a códigos) |
| GET | `/system/internal/roles/{roleId}/permissions/codes` | auth-service |
| GET | `/system/internal/roles/all?page=&size=` | search-service (reindex) |
| GET | `/system/internal/roles/count` | search-service (reindex) |

> Estos endpoints están protegidos por estar en una red privada. **En producción real**, agregar header `X-Internal-Service: <secret>` o mTLS para hardening adicional.

---

## 16. Reindex masivo

Cuando necesites cargar histórico de MySQL → ES (primera vez en un ambiente, después de cambiar mappings, recovery tras corrupción).

### Cómo activar

Edita `saas-config-repo/search-service.properties`:

```properties
saas.search.reindex.enabled=true
saas.search.reindex.entities=all              # o: users,roles
```

Reinicia search-service. En el log verás:

```
Reindex INICIADO. Entidades: [users, roles]
Eureka: todos los services requeridos disponibles [auth-service, system-service]
Reindex users: 145 registros a indexar
Reindex users: 145/145 indexados
Reindex users TERMINADO: 145 indexados, sin fallos
Reindex roles: 4 registros a indexar
Reindex roles: 4/4 indexados
Reindex roles TERMINADO: 4 indexados, sin fallos
Reindex FINALIZADO en 1234 ms
```

> Después de reindexar, **vuelve a poner `enabled=false`** para evitar reindex automático en cada arranque.

### Cómo funciona

1. Search-service espera a que Eureka tenga las instancias requeridas (auth/system).
2. Llama via Feign a `/internal/{entidad}/count` para saber cuántos registros hay.
3. Llama a `/internal/{entidad}/all?page=N&size=500` paginado.
4. Mapea cada `JsonNode` a `Document` y hace `ops.save()` en ES.
5. Loguea progreso por batch + total al final.

---

## 17. Manejo de ambientes (profiles)

### Estructura de archivos

```
saas-config-repo/
├── application.properties              ← global, todos los servicios
├── application-local.properties        ← IntelliJ (default)
├── application-docker.properties       ← stack en Docker
├── application-dev.properties          ← servidor remoto dev
├── application-prod.properties         ← producción (todo via env vars)
├── auth-service.properties             ← config específica de auth
├── system-service.properties
├── search-service.properties
└── gateway-service.properties
```

### Cómo se combinan

Cuando un servicio arranca con profile `X`, Spring obtiene la unión de:
1. `application.properties` (global)
2. `application-X.properties` (ambiente)
3. `<service>.properties` (servicio)
4. `<service>-X.properties` (servicio + ambiente, si existe)

**Las más específicas ganan**.

### Diferencias clave por profile

| Profile | DB host | Eureka host | Redis host | Kafka brokers | ES uri |
|---|---|---|---|---|---|
| **local** | `localhost:3306` | `localhost:8761` | `localhost:6379` | `localhost:9094,9095,9096` | `localhost:9200` |
| **docker** | `mysql:3306` | `discovery-service:8761` | `redis:6379` | `kafka-1:9092,...` | `es-01:9200` |
| **dev** | `${DEV_DB_HOST}` | `${DEV_EUREKA_HOST}` | `${DEV_REDIS_HOST}` | env vars | env vars |
| **prod** | `${DB_URL}` | `${EUREKA_URL}` | `${REDIS_HOST}` | env vars | env vars |

### Activación

| Modo | Cómo se setea |
|---|---|
| **IntelliJ** | `spring.profiles.default=local` en cada `application.properties` del módulo |
| **Docker** | `SPRING_PROFILES_ACTIVE: docker` en `docker-compose.yml` |
| **CLI** | `SPRING_PROFILES_ACTIVE=prod java -jar app.jar` |

---

## 18. Cambiar entre IntelliJ y Docker

### A) Modo IntelliJ (desarrollo)

Solo la infra corre en Docker; los servicios Java en IntelliJ.

```bash
# Apaga MySQL/Redis local si los tienes (puertos 3306, 6379)
docker compose up -d mysql redis kafka-1 kafka-2 kafka-3 es-01
```

En IntelliJ, arrancar en orden:

1. `ConfigServerApplication`
2. `DiscoveryServiceApplication`
3. `AuthServiceApplication`
4. `SystemServiceApplication`
5. `SearchServiceApplication`
6. `GatewayServiceApplication`

> **Tip**: usa Compound Run Config con delays.

### B) Modo Docker (demo / integración)

```bash
docker compose down -v               # opcional: reset total
docker compose build --no-cache      # solo si tocaste código Java
docker compose up -d
```

Los profiles se setean en `docker-compose.yml` (`SPRING_PROFILES_ACTIVE: docker`).

### C) Cómo verificar el modo activo

```bash
# Por log al arrancar:
"The following 1 profile is active: \"local\""    ← IntelliJ
"The following 1 profile is active: \"docker\""   ← Docker

# Por endpoint:
curl http://localhost:8082/auth/api/info | jq .data.environment
```

---

## 19. Rate limiting

Aplicado en gateway, usando Redis como bucket store.

| Ruta | Replenish | Burst | Key |
|---|---|---|---|
| `POST /auth/login` | 2/seg | 5 | **IP** |
| Resto | 20/seg | 40 | userId si autenticado, IP si no |

**Por qué login usa IP**: previene brute force que afectaría el bucket de la víctima.

**Configurar en runtime**: `gateway-service.properties`

```properties
saas.gateway.rate-limit.default.replenish-rate=20
saas.gateway.rate-limit.default.burst-capacity=40
saas.gateway.rate-limit.login.replenish-rate=2
saas.gateway.rate-limit.login.burst-capacity=5
```

Cuando se excede: HTTP 429 con headers `X-RateLimit-Remaining`, `X-RateLimit-Burst-Capacity`.

---

## 20. CORS

Centralizado en gateway (`CorsConfig.java` + `SecurityConfig.java`).

```properties
# gateway-service.properties (defaults)
saas.cors.allowed-origins=http://localhost:4200,http://localhost:3000
saas.cors.allow-credentials=true
saas.cors.max-age-seconds=3600
```

| Profile | Origen típico |
|---|---|
| **local** | `http://localhost:4200,http://127.0.0.1:4200,http://localhost:3000,http://localhost:5173` |
| **dev** | `${DEV_CORS_ORIGINS:http://dev-server:4200}` |
| **prod** | `${CORS_ORIGINS}` (obligatorio, sin default) |

**Patrones**: acepta `https://*.empresa.com` para múltiples subdominios.

**Verificar preflight**:
```bash
curl -i -X OPTIONS http://localhost:8080/auth/login \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST"
```

Esperado: `204 No Content` con headers `Access-Control-Allow-*`.

---

## 21. Datos seed

### Cuenta admin (creada por `DataInitializer` al arrancar auth-service)

```
Username:  admin
Email:     admin@saas.local
Password:  Admin123!
Rol:       ADMIN
```

> ⚠️ **Cambia el password** tras el primer login en cualquier ambiente que no sea local.

### Roles seed (Flyway V2)

| Code | Permisos |
|---|---|
| `ADMIN` | VIEW, CREATE, EDIT, DELETE, EXPORT, IMPORT |
| `USER` | VIEW, CREATE, EDIT |
| `GUEST` | VIEW |

### Listas del sistema seed

| Code | Items |
|---|---|
| `TIPOS_DOCUMENTO` | CC, TI, CE, PA, NIT |
| `ESTADOS_REGISTRO` | ACT, INA, PEN, BLO |
| `GENEROS` | M, F, O |

### Constantes seed

| Code | Value |
|---|---|
| `MAYORIA_EDAD` | `18` |
| `MAX_LOGIN_ATTEMPTS` | `5` |
| `SESION_TIMEOUT_MIN` | `60` |
| `PROFILE_PHOTO_MAX_KB` | `512` |

### Menús seed (V3)

Alineados con la arquitectura del front:
- `/admin/*` para administradores
- `/tenant/*` para usuarios de negocio

---

## 22. Migraciones Flyway

Solo `auth-service` ejecuta Flyway (es el "líder de schema"). Las migraciones viven en `auth-service/src/main/resources/db/migration/`.

### Convenciones

- Versionado: `V<n>__<descripcion>.sql` (doble underscore).
- Una vez aplicada, **NO modificar** — crear `V<n+1>__fix_xxx.sql`.
- Para datos editables (catálogos, menús), usa `INSERT ... ON DUPLICATE KEY UPDATE`.

### Migraciones actuales

| Versión | Archivo | Qué hace |
|---|---|---|
| V1 | `V1__schema.sql` | Esquema completo: users, role, permission, menu, etc. |
| V2 | `V2__seed_base.sql` | Datos seed (roles, permisos, listas, constantes) |
| V3 | `V3__realign_menus.sql` | Re-siembra menús con rutas `/admin/*` y `/tenant/*` |
| V4 | `V4__outbox_event.sql` | Tabla `outbox_event` para el patrón outbox |

### Resetear desde cero (solo dev)

```bash
docker compose down -v                # borra volumen MySQL
docker compose up -d mysql            # arranca limpia
# Al arrancar auth-service, Flyway aplica V1 → V4
```

---

## 23. Agregar entidad nueva indexable

> Ejemplo: agregar entidad `Provider` en system-service y que aparezca en `/search/providers`.

### Checklist completo (~21 archivos)

#### A. Dominio + persistencia (system-service)
1. Migration `V<N>__provider.sql` (en `auth-service/db/migration/`)
2. `ProviderEntity.java` (extends BaseEntity)
3. `Provider.java` (domain, extends BaseDomain)
4. `ProviderPersistenceMapper.java` (extends IBaseMapper)
5. `JpaProviderRepository.java`
6. `IProviderRepositoryPort.java`
7. `ProviderRepositoryAdapter.java` (extends BaseJpaRepositoryAdapter)
8. `IProviderUseCase.java`
9. `ProviderService.java` (extends GenericCrudService o CodeCrudService)

#### B. Eventos (saas-common + system-service)
10. Constantes en `EventTypes.java`: `PROVIDER_CREATED/UPDATED/DELETED`
11. `ProviderEventPayload.java` (DTO plano, sin entidad JPA)
12. En `ProviderService` override `onAfterCreate/Update/Delete` → `outboxPublisher.publish()`

#### C. Endpoints (system-service)
13. `ProviderController.java` (CRUD público)
14. En `InternalController.java` agregar `/providers/all` y `/providers/count`

#### D. Indexación (search-service)
15. `ProviderDocument.java` (extends BaseDocument, `@Document(writeTypeHint=FALSE)`)
16. Constante en `Entities.java`: `PROVIDER_ENTITY = "providers"`
17. Método `providers()` en `IndexNames.java`
18. `providers-mapping.json` en `resources/elasticsearch/`
19. Agregar al `IndexBootstrap.SPECS`
20. `ProviderEventHandler.java` (implements EventHandler)

#### E. API search (search-service)
21. `ProviderSearchService.java` (extends BaseSearchService)
22. Endpoint `GET /search/providers` en `SearchController.java`

#### F. Reindex (opcional)
23. En `SystemInternalClient.java`: `fetchProviders()`, `countProviders()`
24. En `ReindexService`: agregar a `ALL_ENTITIES`, `ENTITY_TO_SERVICE`, y `case` en switch

### Verificación end-to-end

```bash
# Compilar
mvn -pl saas-common,system-service,search-service -am compile -q

# Reiniciar en orden
docker compose restart system-service
docker compose restart search-service

# Verificar índice creado
curl http://localhost:9200/_cat/indices?v   # debe aparecer providers_v1

# Crear y buscar
curl -X POST http://localhost:8080/system/providers \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"code":"PROV1","name":"Proveedor 1"}'

curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/search/providers?q=proveedor"
```

---

## 24. Agregar microservicio nuevo

> Ejemplo: crear `thirdparty-service` para gestionar proveedores y contratos.

### Pasos

1. **Módulo Maven**:
   - Agregar `<module>thirdparty-service</module>` al `pom.xml` raíz.
   - Crear `thirdparty-service/pom.xml` (copia de `system-service`).

2. **Application class**: `ThirdpartyServiceApplication.java` con:
   - `@SpringBootApplication(scanBasePackages = { "com.saas.thirdparty", "com.saas.common" })`
   - `@EnableDiscoveryClient`
   - `@EnableScheduling` (necesario para OutboxRelay)
   - `@EntityScan` y `@EnableJpaRepositories` con `com.saas.common.outbox`

3. **`application.properties`** en el módulo:
   ```properties
   spring.application.name=thirdparty-service
   spring.config.import=optional:configserver:http://localhost:8888/
   spring.profiles.default=local
   ```

4. **`saas-config-repo/thirdparty-service.properties`**:
   ```properties
   server.port=8086
   server.servlet.context-path=/thirdparty
   spring.flyway.enabled=false
   spring.jpa.hibernate.ddl-auto=validate
   ```

5. **`SecurityConfig.java`** (copia de system-service, ajusta paquete).

6. **Gateway routing** en `RouteConfig.java`:
   ```java
   .route("thirdparty", r -> r.path("/thirdparty/**")
           .filters(...)
           .uri(thirdpartyUri))
   ```
   Y agregar `saas.gateway.services.thirdparty-uri` en `application-local.properties` y `application-docker.properties`.

7. **Dockerfile**: agregar STAGE 10 con `target: thirdparty-service`.

8. **`docker-compose.yml`**: agregar service `thirdparty-service` con `mem_limit`, `depends_on` (mysql, kafka-1, etc.), `healthcheck`.

9. **Para cada entidad nueva** dentro: seguir el checklist de la sección 23.

---

## 25. Comandos útiles

### Docker

```bash
# Ver estado de todos los containers
docker compose ps

# Logs de un servicio
docker compose logs -f auth-service

# Reiniciar un servicio
docker compose restart auth-service

# Reconstruir un servicio
docker compose build auth-service
docker compose up -d auth-service

# Detener todo (mantener volúmenes)
docker compose stop

# Detener y eliminar (mantener volúmenes)
docker compose down

# Reset total (BORRA datos)
docker compose down -v
```

### Maven

```bash
# Compilar todo
mvn clean compile -q

# Compilar un módulo + sus dependencias
mvn -pl search-service -am compile -q

# Build con paquete (genera el jar)
mvn -pl search-service -am package -DskipTests -q

# Install en repo local (necesario si search depende de saas-common)
mvn -pl saas-common install -DskipTests -q
```

### Inspeccionar runtime

```bash
# Servicios registrados en Eureka
curl http://localhost:8761/eureka/apps -H "Accept: application/json" | jq

# Rutas activas en gateway
curl http://localhost:8080/actuator/gateway/routes | jq '.[] | {id, uri}'

# Health de cada servicio
for p in 8080 8082 8083 8085; do
  curl -s http://localhost:$p/actuator/health | jq -r '.status'
done

# Topics Kafka
docker exec saas-kafka-1 kafka-topics --bootstrap-server kafka-1:9092 --list

# Consumer lag
docker exec saas-kafka-1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092 \
  --describe --group search-indexer

# Cluster ES
curl http://localhost:9200/_cluster/health | jq

# Índices ES
curl http://localhost:9200/_cat/indices?v

# Aliases ES
curl http://localhost:9200/_cat/aliases?v
```

### Reset BD sin tocar containers

```bash
docker compose exec mysql mysql -u root -prootpassword \
  -e "DROP DATABASE saas_db; CREATE DATABASE saas_db;"
docker compose restart auth-service
```

### Verificación de outbox (debugging)

```bash
# Eventos pendientes
docker exec saas-mysql mysql -uroot -prootpassword saas_db -e \
  "SELECT EventType, Status, Retries FROM outbox_event ORDER BY CreatedAt DESC LIMIT 10"

# Eventos fallidos
docker exec saas-mysql mysql -uroot -prootpassword saas_db -e \
  "SELECT * FROM outbox_event WHERE Status='FAILED' LIMIT 10"
```

### Backup / Restore

```bash
./backup.sh              # crear backup
./backup.sh --list       # listar backups
./backup.sh --restore    # restaurar (interactivo)
```

---

## 26. Troubleshooting

### Servicios no inician / errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `Connection refused: localhost:3306` | MySQL no corriendo | `docker compose up -d mysql` |
| `Connect to localhost:8888 failed` | Config-server no arrancó | Arrancarlo primero |
| `Schema-validation: missing table` | Imagen Docker vieja | `docker compose down -v && docker compose build --no-cache && docker compose up -d` |
| Gateway 503 al hacer cualquier request | Servicio destino no registrado en Eureka | Esperar ~30s o reiniciar |
| Login retorna `roleCodes: []` | Feign no resolvió, system-service caído | Verificar system-service en Eureka |
| `port already in use: 3306` | MySQL local corriendo | Apagar local o cambiar puerto Docker |
| `FlywayException: Validate failed` | Migración aplicada modificada | Crear `V<n+1>__fix.sql` |

### Outbox / Kafka

| Síntoma | Causa | Solución |
|---|---|---|
| `outbox_event` se llena, status PENDING | OutboxRelay no corre | Verificar `@EnableScheduling` + `saas.outbox.relay-enabled=true` |
| Eventos publicados a Kafka pero ES no los recibe | Consumer group mal configurado | `kafka-consumer-groups --describe` |
| Documentos duplicados en ES | Sin dedup Redis | Verificar Redis y `ProcessedEventCache` |
| `No servers available for service: ...` | Eureka aún no descubrió | `ReindexService` ahora espera 60s, suficiente. Si no, reiniciar |
| Consumer atascado en mismo mensaje | Error en handler, retry infinito | Ver log → encontrar bug, posible DLQ |

### Elasticsearch

| Síntoma | Causa | Solución |
|---|---|---|
| ES status `red` | Cluster roto | `GET /_cluster/health?level=indices` |
| ES status `yellow` permanente | Réplicas sin asignar (1 nodo) | Normal en dev. Mappings con `replicas: 0` para green |
| `strict_dynamic_mapping_exception [_class]` | Spring Data ES escribió `_class` | Usar `@Document(writeTypeHint=FALSE)` |
| `strict_dynamic_mapping_exception [id]` | Mapping no incluye `id` | Agregar `"id": { "type": "keyword" }` al JSON |
| Reindex tarda mucho | refresh interval = 1s + replicas = 1 | Bajar a `refresh: 30s, replicas: 0` durante reindex |

### Frontend / CORS

| Síntoma | Causa | Solución |
|---|---|---|
| `No 'Access-Control-Allow-Origin' header` | Origen no listado | Agregar a `saas.cors.allowed-origins` |
| Preflight 401 | Security intercepta OPTIONS | Verificar `.cors(Customizer.withDefaults())` en SecurityConfig |
| 404 sin headers CORS | Path no existe | Verificar ruta en `RouteConfig.java` |
| El front recibe error CORS en cualquier endpoint | El front pega a `/api/...` | Quitar `/api/` del front. Las rutas son limpias |

---

## 27. Producción y hardening

### Antes de salir a producción

1. **Cambiar passwords** en `.env` y todos los seeds.
2. **JWT secret** vía env var (`JWT_SECRET`, mínimo 32 chars).
3. **Activar xpack security en ES**:
   ```yaml
   - xpack.security.enabled=true
   - xpack.security.transport.ssl.enabled=true
   ```
4. **Subir a 3 nodos ES** (descomentar `es-02` y `es-03` en `docker-compose.yml`).
5. **Volver a `replicas: 1`** en los mappings JSON.
6. **HTTPS con reverse proxy** (nginx) frente al gateway.
7. **Eliminar Kafdrop y Kibana** (o ponerlos detrás de auth + VPN).
8. **Cerrar puertos internos** (3306, 6379, 9200, 9094-9096) en el firewall — solo 443 abierto.
9. **Activar fail2ban** para SSH y HTTP.
10. **Backups automáticos** (cron + `backup.sh`).
11. **Monitoreo**:
    - Outbox lag: `count(*) WHERE Status='PENDING' AND CreatedAt < now() - 30s`
    - Kafka consumer lag: `kafka-consumer-groups --describe`
    - ES disk usage: `GET /_cat/allocation?v`
    - JVM heap: `actuator/metrics/jvm.memory.used`

### Capacity planning

| Volumen | Cluster mínimo |
|---|---|
| <1M docs, <100 events/s | 1 ES + 1 Kafka (dev) |
| <10M docs, <1K events/s | 3 ES × 2GB heap, 3 Kafka × 1GB |
| <100M docs, <10K events/s | 5 ES × 4GB heap, 5 Kafka × 2GB |

### Variables de producción típicas

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://prod-db.empresa.com:3306/saas_db?...'
export DB_USERNAME=saasapp
export DB_PASSWORD='<secret>'
export REDIS_HOST=redis.empresa.com
export REDIS_PASSWORD='<secret>'
export EUREKA_URL='http://eureka.empresa.com:8761/eureka/'
export JWT_SECRET='<min-32-chars>'
export JWT_EXPIRATION_MS=900000              # 15 min
export JWT_REFRESH_EXPIRATION_MS=604800000   # 7 días
export ES_JAVA_OPTS='-Xms2g -Xmx2g'
export CORS_ORIGINS='https://app.empresa.com,https://admin.empresa.com'
```

---

## 📎 Apéndices

### A. Smoke test completo (PowerShell)

```powershell
# Login
$resp = Invoke-RestMethod -Uri http://localhost:8080/auth/login -Method POST `
  -ContentType "application/json" `
  -Body '{"usernameOrEmail":"admin","password":"Admin123!"}'
$token = $resp.data.tokens.accessToken
$headers = @{ Authorization = "Bearer $token" }

# Mis menús
Invoke-RestMethod -Uri http://localhost:8080/system/menus/me -Headers $headers | ConvertTo-Json -Depth 6

# Buscar roles en ES
Invoke-RestMethod -Uri "http://localhost:8080/search/roles?q=admin" -Headers $headers | ConvertTo-Json -Depth 4

# Buscar usuarios en ES
Invoke-RestMethod -Uri "http://localhost:8080/search/users?roleCodes=ADMIN" -Headers $headers | ConvertTo-Json -Depth 4

# Catálogo de tipos de documento
Invoke-RestMethod -Uri http://localhost:8080/system/system-lists/code/TIPOS_DOCUMENTO/items -Headers $headers
```

### B. Cluster Kafka — debugging

```bash
# Crear/listar topics
docker exec saas-kafka-1 kafka-topics --bootstrap-server kafka-1:9092 --list

# Detalles de un topic
docker exec saas-kafka-1 kafka-topics --bootstrap-server kafka-1:9092 \
  --describe --topic domain.events

# Producer de consola
docker exec -it saas-kafka-1 kafka-console-producer \
  --bootstrap-server kafka-1:9092 --topic domain.events \
  --property "parse.key=true" --property "key.separator=:"

# Consumer de consola (ver eventos en tiempo real)
docker exec -it saas-kafka-1 kafka-console-consumer \
  --bootstrap-server kafka-1:9092 --topic domain.events --from-beginning

# Reset offset de consumer group (para forzar replay)
docker exec saas-kafka-1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092 --group search-indexer \
  --reset-offsets --to-earliest --topic domain.events --execute
```

### C. Cluster ES — operaciones comunes

```bash
# Ver mappings de un índice
curl http://localhost:9200/users/_mapping?pretty

# Borrar un índice (cuidado!)
curl -X DELETE http://localhost:9200/users_v1

# Forzar refresh
curl -X POST http://localhost:9200/users/_refresh

# Búsqueda directa (debugging)
curl -X POST http://localhost:9200/users/_search?pretty -H 'Content-Type: application/json' -d '{
  "query": { "match": { "fullName": "juan" } }
}'
```

---

## Licencia

Propietario — Uso interno.

---

**Para deploy en VPS**: ver [`DEPLOY-README.md`](./DEPLOY-README.md).
