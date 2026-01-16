# 🚀 Sistema de Despliegue Dinámico y Escalable

Sistema completamente dinámico que detecta automáticamente tus microservicios sin necesidad de modificar código. **Agrega 10, 20 o 100 microservicios solo editando configuración.**

## 🎯 Características Principales

✅ **Totalmente Dinámico** - Detecta automáticamente servicios del config
✅ **Escalable** - Agrega N microservicios sin tocar código
✅ **Auto-detección** - Lee puertos y dependencias automáticamente
✅ **Health Checks Inteligentes** - Verifica cada servicio individualmente
✅ **Despliegue Selectivo** - Despliega solo los servicios que necesites
✅ **Rollback Fácil** - Vuelve a versión anterior en minutos
✅ **SSH Seguro** - Conexión sin password con claves
✅ **Logs en Tiempo Real** - Ve el progreso del despliegue

## 📁 Archivos del Sistema

```
.
├── remote-deploy.sh          # Script principal (tu PC)
├── deploy-selective.sh       # Script dinámico (VPS)
├── deploy-config.env         # Configuración de servicios
└── setup-ssh.sh             # Configurar SSH (una vez)
```

## 🔧 Setup Inicial (Una Sola Vez)

### Paso 1: Configurar SSH

```bash
chmod +x setup-ssh.sh remote-deploy.sh deploy-selective.sh
./setup-ssh.sh
```

Esto configurará acceso SSH sin password.

### Paso 2: Crear Configuración

```bash
./remote-deploy.sh
```

En la primera ejecución, creará `deploy-config.env` automáticamente.

### Paso 3: Editar Configuración

```bash
nano deploy-config.env
```

## 📝 Formato de Configuración

### Para Cada Microservicio, Define 3 Variables:

```bash
# 1. Habilitar/deshabilitar
DEPLOY_MI_SERVICIO=true

# 2. Puerto (para health check)
SERVICE_PORT_MI_SERVICIO=8089

# 3. Dependencias (servicios que deben estar UP primero)
SERVICE_DEPS_MI_SERVICIO="mysql config-server discovery-service"
```

### Ejemplo Completo:

```bash
# ============================================
# NUEVO MICROSERVICIO: Notification Service
# ============================================

DEPLOY_NOTIFICATION_SERVICE=true
SERVICE_PORT_NOTIFICATION_SERVICE=8084
SERVICE_DEPS_NOTIFICATION_SERVICE="mysql config-server discovery-service"
```

**¡ESO ES TODO!** El sistema lo detectará automáticamente.

## 🚀 Agregar un Nuevo Microservicio

### Método Simple (3 Líneas)

1. **Edita `deploy-config.env`:**

```bash
# Payment Service
DEPLOY_PAYMENT_SERVICE=true
SERVICE_PORT_PAYMENT_SERVICE=8085
SERVICE_DEPS_PAYMENT_SERVICE="mysql config-server discovery-service auth-service"
```

2. **Asegúrate que existe en `docker-compose.yml`:**

```yaml
payment-service:
  build:
    context: .
    dockerfile: Dockerfile
    target: payment-service
  ports:
    - "8085:8085"
  # ... resto de config
```

3. **Despliega:**

```bash
./remote-deploy.sh
```

**¡Listo!** El sistema automáticamente:
- ✅ Detecta el nuevo servicio
- ✅ Lo construye
- ✅ Lo despliega
- ✅ Verifica sus dependencias
- ✅ Hace health check
- ✅ Muestra logs si falla

### Ejemplo: Agregar 5 Microservicios Nuevos

```bash
# En deploy-config.env:

# Notification Service
DEPLOY_NOTIFICATION_SERVICE=true
SERVICE_PORT_NOTIFICATION_SERVICE=8084
SERVICE_DEPS_NOTIFICATION_SERVICE="mysql config-server discovery-service"

# Payment Service  
DEPLOY_PAYMENT_SERVICE=true
SERVICE_PORT_PAYMENT_SERVICE=8085
SERVICE_DEPS_PAYMENT_SERVICE="mysql config-server discovery-service auth-service"

# Analytics Service
DEPLOY_ANALYTICS_SERVICE=true
SERVICE_PORT_ANALYTICS_SERVICE=8086
SERVICE_DEPS_ANALYTICS_SERVICE="config-server discovery-service"

# Customer Service
DEPLOY_CUSTOMER_SERVICE=true
SERVICE_PORT_CUSTOMER_SERVICE=8087
SERVICE_DEPS_CUSTOMER_SERVICE="mysql config-server discovery-service auth-service"

# Inventory Service
DEPLOY_INVENTORY_SERVICE=true
SERVICE_PORT_INVENTORY_SERVICE=8088
SERVICE_DEPS_INVENTORY_SERVICE="mysql config-server discovery-service"
```

```bash
./remote-deploy.sh
```

El sistema desplegará los 5 servicios automáticamente.

## 🎯 Casos de Uso

### Caso 1: Despliegue Completo

```bash
# En deploy-config.env: todos en true
./remote-deploy.sh
```

### Caso 2: Solo Actualizar Gateway

```bash
# En deploy-config.env:
DEPLOY_GATEWAY_SERVICE=true
# Todo lo demás en false
./remote-deploy.sh
```

### Caso 3: Actualizar 3 Servicios Específicos

```bash
# En deploy-config.env:
DEPLOY_AUTH_SERVICE=true
DEPLOY_PAYMENT_SERVICE=true
DEPLOY_NOTIFICATION_SERVICE=true
# El resto en false
./remote-deploy.sh
```

### Caso 4: Despliegue Desde Cero (Force Rebuild)

```bash
# En deploy-config.env:
FORCE_REBUILD=true
# Todos los servicios que quieras en true
./remote-deploy.sh

# Después vuelve a:
FORCE_REBUILD=false
```

### Caso 5: Desplegar Sin Backup (Desarrollo)

```bash
# En deploy-config.env:
SKIP_BACKUP=true
./remote-deploy.sh
```

### Caso 6: Desplegar Desde Otra Rama

```bash
# En deploy-config.env:
GIT_BRANCH=develop
./remote-deploy.sh
```

## 🔍 Monitoreo y Debugging

### Ver Logs en Tiempo Real

```bash
# Todos los servicios
ssh saas-vps "cd /opt/saas-platform && docker compose logs -f"

# Servicio específico
ssh saas-vps "cd /opt/saas-platform && docker compose logs -f payment-service"

# Últimas 100 líneas
ssh saas-vps "cd /opt/saas-platform && docker compose logs --tail=100"
```

### Ver Estado de Servicios

```bash
ssh saas-vps "cd /opt/saas-platform && docker compose ps"
```

### Verificar Health Check Manual

```bash
# Desde el VPS
curl http://localhost:8085/actuator/health

# Desde tu PC (si tienes acceso)
curl http://72.62.174.193:8085/actuator/health
```

### Verificar en Eureka

```bash
# Abrir en navegador
http://72.62.174.193:8761
```

## 🔄 Rollback Rápido

### Método 1: Git Revert

```bash
# En tu PC
git log --oneline  # Ver commits
git revert abc123  # Revertir commit específico
git push

# En deploy-config.env:
FORCE_REBUILD=true

./remote-deploy.sh
```

### Método 2: Checkout Anterior

```bash
# En deploy-config.env:
GIT_BRANCH=v1.2.3  # Tag o branch anterior
FORCE_REBUILD=true

./remote-deploy.sh
```

## 🛠️ Troubleshooting

### Problema: Servicio No Inicia

```bash
# 1. Ver logs
ssh saas-vps "cd /opt/saas-platform && docker compose logs payment-service"

# 2. Verificar contenedor
ssh saas-vps "docker ps -a | grep payment"

# 3. Reiniciar servicio
ssh saas-vps "cd /opt/saas-platform && docker compose restart payment-service"
```

### Problema: Health Check Timeout

```bash
# Aumenta el timeout en deploy-config.env:
HEALTH_CHECK_TIMEOUT=180  # De 120 a 180 segundos
```

### Problema: Dependencias Circulares

```bash
# Revisa SERVICE_DEPS_* en deploy-config.env
# Asegúrate que no haya ciclos:
# ❌ MAL: A depende de B, B depende de A
# ✅ BIEN: Orden jerárquico claro
```

### Problema: Out of Memory

```bash
# Aumenta memoria en docker-compose.yml:
deploy:
  resources:
    limits:
      memory: 1G
    reservations:
      memory: 512M
```

### Problema: Puerto Ya En Uso

```bash
# Verificar qué usa el puerto
ssh saas-vps "netstat -tlnp | grep 8085"

# Cambiar puerto en docker-compose.yml y deploy-config.env
```

## 🔐 Seguridad

### Mejores Prácticas

1. **Usar Claves SSH (No Passwords)**
```bash
./setup-ssh.sh  # Solo una vez
```

2. **No Versionar Credenciales**
```bash
# .gitignore
deploy-config.env  # Si contiene secretos
.ssh/
*.key
```

3. **Variables de Entorno para Secretos**
```bash
# En VPS, crea /opt/saas-platform/.env.production
DB_PASSWORD=secret123
JWT_SECRET=supersecret
```

4. **Deshabilitar Password Login en VPS**
```bash
ssh saas-vps
sudo nano /etc/ssh/sshd_config

# Cambiar:
PasswordAuthentication no
PermitRootLogin prohibit-password

sudo systemctl restart sshd
```

## 📊 Estructura Recomendada

### Para 10+ Microservicios

```bash
# deploy-config.env

# ============================================
# CAPA 1: INFRAESTRUCTURA (Siempre primero)
# ============================================
DEPLOY_MYSQL=true
DEPLOY_CONFIG_SERVER=true
SERVICE_PORT_CONFIG_SERVER=8888

DEPLOY_DISCOVERY_SERVICE=true
SERVICE_PORT_DISCOVERY_SERVICE=8761
SERVICE_DEPS_DISCOVERY_SERVICE="config-server"

# ============================================
# CAPA 2: SERVICIOS BASE (Sin dependencias externas)
# ============================================
DEPLOY_AUTH_SERVICE=true
SERVICE_PORT_AUTH_SERVICE=8082
SERVICE_DEPS_AUTH_SERVICE="mysql config-server discovery-service"

# ============================================
# CAPA 3: SERVICIOS DE NEGOCIO
# ============================================
DEPLOY_PAYMENT_SERVICE=true
SERVICE_PORT_PAYMENT_SERVICE=8085
SERVICE_DEPS_PAYMENT_SERVICE="mysql config-server discovery-service auth-service"

DEPLOY_NOTIFICATION_SERVICE=true
SERVICE_PORT_NOTIFICATION_SERVICE=8084
SERVICE_DEPS_NOTIFICATION_SERVICE="mysql config-server discovery-service"

# ... más servicios

# ============================================
# CAPA 4: GATEWAY (Siempre último)
# ============================================
DEPLOY_GATEWAY_SERVICE=true
SERVICE_PORT_GATEWAY_SERVICE=8080
SERVICE_DEPS_GATEWAY_SERVICE="config-server discovery-service auth-service"
```

## 🎯 Tips Pro

### 1. Alias Útiles

```bash
# Agregar a ~/.bashrc o ~/.zshrc:
alias deploy='./remote-deploy.sh'
alias vps='ssh saas-vps'
alias vps-logs='ssh saas-vps "cd /opt/saas-platform && docker compose logs -f"'
alias vps-ps='ssh saas-vps "cd /opt/saas-platform && docker compose ps"'
alias vps-restart='ssh saas-vps "cd /opt/saas-platform && docker compose restart"'
```

### 2. Script de Status Rápido

```bash
# check-status.sh
#!/bin/bash
ssh saas-vps "cd /opt/saas-platform && docker compose ps && echo '' && curl -s http://localhost:8761/eureka/apps | grep -o '<name>[^<]*</name>' | sed 's/<[^>]*>//g'"
```

### 3. Notificaciones de Despliegue

```bash
# Al final de remote-deploy.sh:
if [ $DEPLOY_EXIT_CODE -eq 0 ]; then
    # Slack, Discord, Email, etc.
    curl -X POST https://hooks.slack.com/... -d '{"text":"✅ Deploy exitoso"}'
fi
```

### 4. Configuración por Ambiente

```bash
# deploy-config.dev.env
# deploy-config.staging.env  
# deploy-config.prod.env

./remote-deploy.sh -c deploy-config.prod.env
```

## 📈 Métricas y Monitoreo

### Prometheus + Grafana (Opcional)

```bash
# Agregar a deploy-config.env:
DEPLOY_PROMETHEUS=true
SERVICE_PORT_PROMETHEUS=9090
SERVICE_DEPS_PROMETHEUS=""

DEPLOY_GRAFANA=true
SERVICE_PORT_GRAFANA=3000
SERVICE_DEPS_GRAFANA="prometheus"
```

## 🆘 Ayuda Rápida

```bash
# ❓ ¿Cómo agrego un servicio?
# 1. Agrega 3 líneas en deploy-config.env
# 2. Asegura que existe en docker-compose.yml
# 3. ./remote-deploy.sh

# ❓ ¿Cómo despliego solo uno?
# 1. Pon todos en false excepto el que quieres
# 2. ./remote-deploy.sh

# ❓ ¿Cómo hago rollback?
# 1. git revert <commit>
# 2. FORCE_REBUILD=true
# 3. ./remote-deploy.sh

# ❓ ¿Cómo veo logs?
ssh saas-vps "cd /opt/saas-platform && docker compose logs -f <servicio>"

# ❓ ¿Cómo reinicio un servicio?
ssh saas-vps "cd /opt/saas-platform && docker compose restart <servicio>"
```