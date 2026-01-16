#!/bin/bash

# ============================================
# SCRIPT DE DESPLIEGUE DINÁMICO Y ESCALABLE
# Lee servicios del config y los despliega automáticamente
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CONFIG_FILE="deploy-config.env"
BACKUP_DIR="/opt/saas-backups"
LOG_FILE="/var/log/saas-deploy.log"

# Funciones
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🚀 DESPLIEGUE DINÁMICO - VPS            ║
║   Soporta N microservicios                ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Cargar configuración
if [ ! -f "$CONFIG_FILE" ]; then
    error "❌ No se encontró $CONFIG_FILE"
fi

source "$CONFIG_FILE"

# ============================================
# PARSEAR SERVICIOS DINÁMICAMENTE
# ============================================

# Arrays para almacenar servicios
declare -a SERVICES_TO_BUILD
declare -a SERVICES_TO_START
declare -a SERVICES_WITH_HEALTH
declare -A SERVICE_PORTS
declare -A SERVICE_DEPENDENCIES

log "🔍 Analizando configuración de servicios..."

# Leer todas las variables DEPLOY_* del archivo
while IFS='=' read -r key value; do
    # Ignorar comentarios y líneas vacías
    [[ $key =~ ^#.*$ ]] && continue
    [[ -z $key ]] && continue

    # Buscar variables DEPLOY_*
    if [[ $key =~ ^DEPLOY_(.+)$ ]]; then
        service_name="${BASH_REMATCH[1]}"

        # Limpiar el valor (remover espacios y comillas)
        value=$(echo "$value" | tr -d ' "' | tr '[:upper:]' '[:lower:]')

        if [ "$value" = "true" ]; then
            log "   ✓ $service_name habilitado"

            # Convertir nombre a formato docker-compose
            # MYSQL -> mysql
            # AUTH_SERVICE -> auth-service
            docker_service=$(echo "$service_name" | tr '[:upper:]' '[:lower:]' | tr '_' '-')

            # Agregar a lista de servicios a iniciar
            SERVICES_TO_START+=("$docker_service")

            # Si no es mysql, agregarlo también a build
            if [ "$docker_service" != "mysql" ]; then
                SERVICES_TO_BUILD+=("$docker_service")
                SERVICES_WITH_HEALTH+=("$docker_service")
            fi

            # Detectar puerto y dependencias del servicio
            # Buscar variables SERVICE_PORT_* y SERVICE_DEPS_*
            port_var="SERVICE_PORT_${service_name}"
            deps_var="SERVICE_DEPS_${service_name}"

            if [ -n "${!port_var}" ]; then
                SERVICE_PORTS["$docker_service"]="${!port_var}"
            fi

            if [ -n "${!deps_var}" ]; then
                SERVICE_DEPENDENCIES["$docker_service"]="${!deps_var}"
            fi
        fi
    fi
done < "$CONFIG_FILE"

# Si no hay servicios para desplegar, error
if [ ${#SERVICES_TO_START[@]} -eq 0 ]; then
    error "❌ No hay servicios habilitados para desplegar. Revisa $CONFIG_FILE"
fi

log "✅ Servicios detectados: ${SERVICES_TO_START[*]}"
log "🏗️  Servicios a construir: ${SERVICES_TO_BUILD[*]}"

# ============================================
# GIT PULL
# ============================================

if [ "$AUTO_PULL" = "true" ]; then
    log "📥 Actualizando código desde Git..."
    git fetch origin 2>&1 | tee -a "$LOG_FILE"
    git checkout "$GIT_BRANCH" 2>&1 | tee -a "$LOG_FILE"
    git pull origin "$GIT_BRANCH" 2>&1 | tee -a "$LOG_FILE" || warning "⚠️  Git pull falló, continuando con código actual"
    COMMIT_HASH=$(git rev-parse --short HEAD)
    log "✅ Código en commit: $COMMIT_HASH"
fi

# ============================================
# BACKUP DE BASE DE DATOS
# ============================================

should_backup=false
for service in "${SERVICES_TO_START[@]}"; do
    if [ "$service" = "mysql" ]; then
        should_backup=true
        break
    fi
done

if [ "$SKIP_BACKUP" != "true" ] && [ "$should_backup" = "true" ]; then
    if docker ps 2>/dev/null | grep -q saas-mysql; then
        log "📦 Creando backup de MySQL..."
        mkdir -p "$BACKUP_DIR"
        BACKUP_FILE="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

        if docker exec saas-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} > /dev/null 2>&1; then
            docker exec saas-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > "$BACKUP_FILE" 2>/dev/null || true
            if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
                gzip "$BACKUP_FILE"
                SIZE=$(du -h "${BACKUP_FILE}.gz" | cut -f1)
                log "✅ Backup creado: ${BACKUP_FILE}.gz ($SIZE)"
            fi
        else
            warning "⚠️  MySQL no responde, saltando backup"
        fi
    else
        log "ℹ️  MySQL no está corriendo, saltando backup"
    fi
fi

# ============================================
# BUILD DE SERVICIOS
# ============================================

if [ ${#SERVICES_TO_BUILD[@]} -gt 0 ]; then
    log "🏗️  Construyendo servicios (${#SERVICES_TO_BUILD[@]} servicios)..."

    BUILD_FLAGS="--pull"
    if [ "$FORCE_REBUILD" = "true" ]; then
        BUILD_FLAGS="--no-cache --pull"
        log "   ⚡ Modo: Reconstrucción completa (no-cache)"
    fi

    # Construir todos los servicios habilitados
    log "   📦 Construyendo: ${SERVICES_TO_BUILD[*]}"
    docker compose build $BUILD_FLAGS ${SERVICES_TO_BUILD[@]} 2>&1 | tee -a "$LOG_FILE"

    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        error "❌ Error al construir servicios"
    fi

    log "✅ Servicios construidos exitosamente"
else
    log "ℹ️  No hay servicios que construir (solo infraestructura)"
fi

# ============================================
# DETENER SERVICIOS ANTIGUOS
# ============================================

log "🛑 Deteniendo servicios antiguos..."

# Obtener servicios actualmente corriendo
RUNNING_SERVICES=$(docker compose ps --services 2>/dev/null || echo "")

if [ -n "$RUNNING_SERVICES" ]; then
    # Detener solo los servicios que vamos a actualizar
    SERVICES_TO_STOP=()
    for service in ${SERVICES_TO_BUILD[@]}; do
        if echo "$RUNNING_SERVICES" | grep -q "^${service}$"; then
            SERVICES_TO_STOP+=("$service")
        fi
    done

    if [ ${#SERVICES_TO_STOP[@]} -gt 0 ]; then
        log "   🛑 Deteniendo: ${SERVICES_TO_STOP[*]}"
        docker compose stop ${SERVICES_TO_STOP[@]} 2>&1 | tee -a "$LOG_FILE" || true
    else
        log "   ℹ️  No hay servicios previos que detener"
    fi
else
    log "   ℹ️  No hay servicios corriendo (primera ejecución)"
fi

# ============================================
# INICIAR SERVICIOS
# ============================================

log "🚀 Iniciando servicios (${#SERVICES_TO_START[@]} servicios)..."
log "   📋 Orden de inicio: ${SERVICES_TO_START[*]}"

# Iniciar todos los servicios habilitados
docker compose up -d ${SERVICES_TO_START[@]} 2>&1 | tee -a "$LOG_FILE"

if [ $? -ne 0 ]; then
    error "❌ Error al iniciar servicios"
fi

log "✅ Servicios iniciados"

# ============================================
# HEALTH CHECKS DINÁMICOS
# ============================================

log "⏳ Esperando que los servicios estén listos..."
log "   ℹ️  Esto puede tardar 2-4 minutos dependiendo del número de servicios"

wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=${3:-40}
    local attempt=0
    local endpoint="/actuator/health"

    printf "   %-25s " "Esperando $service..."

    while [ $attempt -lt $max_attempts ]; do
        # Intentar conexión
        if curl -sf "http://localhost:$port$endpoint" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ OK (puerto $port)${NC}"
            return 0
        fi

        # Verificar si el contenedor sigue corriendo
        if ! docker compose ps "$service" 2>/dev/null | grep -q "Up"; then
            echo -e "${RED}❌ CRASHED${NC}"
            warning "   $service se detuvo inesperadamente. Ver logs: docker compose logs $service"
            return 1
        fi

        attempt=$((attempt + 1))
        sleep 3
    done

    echo -e "${YELLOW}⚠️  TIMEOUT${NC}"
    warning "   $service no respondió en $(($max_attempts * 3)) segundos"
    return 1
}

# Espera inicial para que los contenedores arranquen
sleep 10

# Array para almacenar servicios que fallaron
declare -a FAILED_SERVICES

# Verificar cada servicio con health check
for service in "${SERVICES_WITH_HEALTH[@]}"; do
    # Obtener puerto del servicio
    port="${SERVICE_PORTS[$service]}"

    # Si no hay puerto configurado, intentar auto-detectar puertos comunes
    if [ -z "$port" ]; then
        case "$service" in
            config-server) port=8888 ;;
            discovery-service) port=8761 ;;
            gateway-service) port=8080 ;;
            auth-service) port=8082 ;;
            system-service) port=8083 ;;
            *-service)
                # Intentar extraer puerto del docker-compose.yml si existe
                port=$(grep -A 5 "^  $service:" docker-compose.yml 2>/dev/null | grep "^\s*-\s*\".*:.*\"" | head -1 | sed 's/.*"\(.*\):.*/\1/' || echo "")
                if [ -z "$port" ]; then
                    warning "   ⚠️  Puerto no configurado para $service, saltando health check"
                    continue
                fi
                ;;
            *)
                warning "   ⚠️  Puerto no configurado para $service, saltando health check"
                continue
                ;;
        esac
    fi

    # Esperar el servicio
    if ! wait_for_service "$service" "$port"; then
        FAILED_SERVICES+=("$service")
    fi
done

# ============================================
# VERIFICAR DEPENDENCIAS (si están configuradas)
# ============================================

log "🔗 Verificando dependencias entre servicios..."

for service in "${!SERVICE_DEPENDENCIES[@]}"; do
    deps="${SERVICE_DEPENDENCIES[$service]}"
    log "   $service requiere: $deps"

    # Verificar que las dependencias estén corriendo
    for dep in $deps; do
        if ! docker compose ps "$dep" 2>/dev/null | grep -q "Up"; then
            warning "   ⚠️  Dependencia $dep de $service no está corriendo"
        fi
    done
done

# ============================================
# LIMPIEZA
# ============================================

log "🧹 Limpiando recursos no utilizados..."
docker image prune -f > /dev/null 2>&1
docker volume prune -f > /dev/null 2>&1 || true

# ============================================
# ESTADO FINAL
# ============================================

log "📊 Estado final de los servicios:"
echo ""
docker compose ps | tee -a "$LOG_FILE"
echo ""

# Verificar cuántos servicios están UP
TOTAL_SERVICES=${#SERVICES_TO_START[@]}
UP_SERVICES=$(docker compose ps | grep -c "Up" || echo "0")

log "📈 Resumen: $UP_SERVICES de $TOTAL_SERVICES servicios UP"

# ============================================
# REPORTE FINAL
# ============================================

echo ""
if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    echo -e "${YELLOW}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   ⚠️  DESPLIEGUE CON ADVERTENCIAS         ║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════╝${NC}"
    echo ""
    warning "Servicios que no pasaron health check: ${FAILED_SERVICES[*]}"
    warning "Revisa los logs: docker compose logs <servicio>"
else
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ✅ DESPLIEGUE COMPLETADO                ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
fi

echo ""
log "🎉 Despliegue completado"
log "📍 Servicios desplegados: ${SERVICES_TO_START[*]}"
log ""
log "📝 Comandos útiles:"
log "   Ver logs:          docker compose logs -f"
log "   Ver servicio:      docker compose logs -f <servicio>"
log "   Ver estado:        docker compose ps"
log "   Reiniciar todo:    docker compose restart"
log "   Reiniciar uno:     docker compose restart <servicio>"

# Si hay servicios con problemas, mostrar sus logs
if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    echo ""
    log "🔍 Logs recientes de servicios con problemas:"
    for service in "${FAILED_SERVICES[@]}"; do
        echo ""
        echo -e "${YELLOW}=== Logs de $service ===${NC}"
        docker compose logs --tail=30 "$service" 2>&1 | tee -a "$LOG_FILE"
    done
fi

echo ""

# Exit code basado en el éxito
if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    exit 1
else
    exit 0
fi