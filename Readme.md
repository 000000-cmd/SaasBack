# SAAS Platform - Línea Base para Microservicios

Sistema de microservicios con Spring Boot 3.5 y Spring Cloud 2025, diseñado como línea base reutilizable para múltiples proyectos.

## Arquitectura

```
                    ┌─────────────────────────────────────┐
                    │         GATEWAY (8080)              │
                    │    JWT Filter + Routing + CORS      │
                    └─────────────────┬───────────────────┘
                                      │
           ┌──────────────────────────┼──────────────────────────┐
           │                          │                          │
           ▼                          ▼                          ▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│   AUTH-SERVICE   │      │  SYSTEM-SERVICE  │      │  (Futuros...)    │
│     (8082)       │      │     (8083)       │      │                  │
│  - Login         │      │  - Listas        │      │                  │
│  - Usuarios      │      │  - Menús         │      │                  │
│  - JWT           │      │  - Constantes    │      │                  │
└────────┬─────────┘      └────────┬─────────┘      └──────────────────┘
         │                         │
         └────────────┬────────────┘
                      │
              ┌───────▼───────┐
              │    MySQL      │
              │    (3306)     │
              └───────────────┘
                      ▲
    ┌─────────────────┴─────────────────┐
    │                                   │
┌───┴────────────┐         ┌───────────┴────┐
│ CONFIG-SERVER  │         │ DISCOVERY      │
│    (8888)      │         │ (Eureka 8761)  │
└────────────────┘         └────────────────┘
```

## Servicios

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| MySQL | 3306 | Base de datos principal |
| Config Server | 8888 | Configuración centralizada |
| Discovery (Eureka) | 8761 | Registro de servicios |
| Auth Service | 8082 | Autenticación, login, usuarios, JWT |
| System Service | 8083 | Configuración del sistema: listas, menús, constantes |
| Gateway | 8080 | Punto de entrada, routing, validación JWT |

## Requisitos

- Docker 24+ y Docker Compose v2
- 4GB RAM mínimo (8GB recomendado)
- Git

## Inicio Rápido

### 1. Clonar repositorio

```bash
git clone https://github.com/tu-usuario/saas-platform.git
cd saas-platform
```

### 2. Configurar variables de entorno

```bash
cp .env.template .env
nano .env
```

Editar y cambiar al menos:
- `MYSQL_ROOT_PASSWORD`: Password seguro para MySQL

### 3. Desplegar

```bash
chmod +x deploy.sh
./deploy.sh
```

### 4. Verificar

```bash
./check-services.sh
```

O acceder a:
- Gateway: http://localhost:8080/actuator/health
- Eureka: http://localhost:8761

## Deployment Remoto

Para desplegar desde tu PC local al servidor VPS:

### Linux/Mac

```bash
# Primera vez: configurar SSH keys
./remote-deploy.sh --setup-ssh

# Desplegar
./remote-deploy.sh
```

### Windows

```batch
remote-deploy.bat
```

### Configuración

Editar `deploy-config.env` para:
- Cambiar host/usuario del VPS
- Habilitar/deshabilitar servicios
- Configurar opciones de build

## Estructura del Proyecto

```
saas-platform/
├── auth-service/           # Servicio de autenticación
├── system-service/         # Servicio de configuración del sistema
├── gateway-service/        # API Gateway
├── config-server/          # Servidor de configuración
├── discovery-service/      # Eureka Server
├── saas-common/            # Librería compartida
├── saas-config-repo/       # Archivos de configuración
│   ├── application.properties
│   ├── auth-service.properties
│   ├── system-service.properties
│   └── gateway-service.properties
├── init-scripts/           # Scripts SQL de inicialización
├── docker-compose.yml
├── Dockerfile
├── deploy.sh               # Script de deployment
├── deploy-config.env       # Configuración de deployment
├── backup.sh               # Script de backup
└── check-services.sh       # Verificación de servicios
```

## Comandos Útiles

```bash
# Ver logs de todos los servicios
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f auth-service

# Reiniciar un servicio
docker compose restart auth-service

# Ver estado de servicios
docker compose ps

# Detener todo
docker compose down

# Reconstruir un servicio
docker compose build auth-service
docker compose up -d auth-service

# Backup de base de datos
./backup.sh

# Restaurar backup
./backup.sh --restore

# Ver backups disponibles
./backup.sh --list
```

## Endpoints Principales

### Auth Service (8082)

```
POST /api/auth/login          # Login
POST /api/auth/refresh        # Refresh token
POST /api/auth/logout         # Logout
GET  /api/auth/me             # Usuario actual
POST /api/users/create        # Crear usuario
```

### System Service (8083)

```
# Roles
GET    /api/system/roles
POST   /api/system/roles
PUT    /api/system/roles/{code}
DELETE /api/system/roles/{code}

# Menús
GET    /api/system/menus
POST   /api/system/menus
PUT    /api/system/menus/{code}
DELETE /api/system/menus/{code}

# Role-Menu (Permisos)
GET    /api/system/role-menus/role/{roleCode}
POST   /api/system/role-menus
DELETE /api/system/role-menus/{id}

# Constantes
GET    /api/system/constants/{code}
POST   /api/system/constants
PUT    /api/system/constants/{code}
DELETE /api/system/constants/{code}
```

### Gateway (8080)

Todas las peticiones a través del gateway:
```
http://localhost:8080/auth-service/api/auth/login
http://localhost:8080/system-service/api/system/roles
```

## Agregar Nuevo Microservicio

1. Crear módulo Maven:
```bash
mkdir nuevo-service
# Copiar estructura de system-service como base
```

2. Agregar al `pom.xml` padre:
```xml
<modules>
    ...
    <module>nuevo-service</module>
</modules>
```

3. Agregar stage en `Dockerfile`:
```dockerfile
FROM base-runtime AS nuevo-service
COPY --from=builder --chown=appuser:appgroup /app/nuevo-service/target/*.jar app.jar
...
```

4. Agregar en `docker-compose.yml`:
```yaml
nuevo-service:
  build:
    context: .
    dockerfile: Dockerfile
    target: nuevo-service
  ...
```

5. Agregar en `deploy-config.env`:
```bash
DEPLOY_NUEVO_SERVICE=true
SERVICE_PORT_NUEVO_SERVICE=8084
SERVICE_DEPS_NUEVO_SERVICE="mysql config-server discovery-service"
```

## Troubleshooting

### Servicio no inicia

```bash
# Ver logs del servicio
docker compose logs nuevo-service --tail=100

# Verificar que dependencias están healthy
docker compose ps
```

### Error de conexión a MySQL

```bash
# Verificar MySQL
docker exec saas-mysql mysqladmin ping -uroot -prootpassword

# Ver logs de MySQL
docker compose logs mysql --tail=50
```

### Puerto ya en uso

```bash
# Verificar qué usa el puerto
netstat -tuln | grep 8080
# o
ss -tuln | grep 8080

# Detener proceso o cambiar puerto en docker-compose.yml
```

### Memoria insuficiente

Reducir memoria en `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xms128m -Xmx256m ..."
```

## Seguridad en Producción

1. **Cambiar passwords** en `.env`
2. **Configurar HTTPS** con Nginx reverse proxy
3. **Cerrar puertos** internos (dejar solo 80, 443, 22)
4. **Configurar fail2ban**
5. **Backups automáticos** (ya configurado con cron)

## Licencia

Propietario - Uso interno

---

Para más información, ver `DEPLOY-README.md`