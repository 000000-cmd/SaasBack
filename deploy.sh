#!/bin/bash

# ============================================
# SCRIPT DE DEPLOYMENT AUTOMÁTICO
# ============================================

set -e  # Exit on error

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
REPO_DIR="/opt/saas-platform"
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
║   🚀 SAAS PLATFORM DEPLOYMENT SCRIPT      ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Verificar que estamos en el directorio correcto
cd "$REPO_DIR" || error "No se puede acceder a $REPO_DIR"

# Verificar que existe .env
if [ ! -f .env ]; then
    error "❌ Archivo .env no encontrado"
fi

# Cargar variables de entorno
set -a
source .env
set +a

# 1. BACKUP (solo si MySQL existe)
log "📦 Verificando si MySQL necesita backup..."
mkdir -p "$BACKUP_DIR"

if docker ps 2>/dev/null | grep -q saas-mysql; then
    log "   MySQL detectado, creando backup..."
    BACKUP_FILE="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

    if docker exec saas-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} > /dev/null 2>&1; then
        docker exec saas-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > "$BACKUP_FILE" 2>/dev/null || true
        if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
            log "   ✅ Backup creado: $BACKUP_FILE"
        else
            warning "   ⚠️  Backup falló, pero continuando..."
        fi
    else
        warning "   ⚠️  MySQL no responde, saltando backup"
    fi
else
    log "   ℹ️  MySQL no existe aún (primera ejecución), saltando backup"
fi

# 2. PULL LATEST CODE
log "📥 Obteniendo últimos cambios del repositorio..."
git fetch origin 2>&1 | tee -a "$LOG_FILE"
git pull origin main 2>&1 | tee -a "$LOG_FILE" || warning "Error al hacer pull (puede ser normal si no hay cambios)"

COMMIT_HASH=$(git rev-parse --short HEAD)
log "✅ Código en commit: $COMMIT_HASH"

# 3. BUILD IMAGES
log "🏗️  Construyendo imágenes Docker (esto puede tardar 10-15 minutos la primera vez)..."
log "   📝 Tip: Puedes ver el progreso en otra terminal con: docker compose logs -f"

docker compose build --pull 2>&1 | tee -a "$LOG_FILE"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    error "❌ Error al construir las imágenes. Revisa los logs arriba."
fi
log "✅ Imágenes construidas exitosamente"

# 4. STOP OLD CONTAINERS (excepto MySQL si existe)
if docker ps 2>/dev/null | grep -q "saas-"; then
    log "🛑 Deteniendo servicios antiguos..."
    docker compose stop config-server discovery-service auth-service system-service gateway-service 2>/dev/null || true
    log "✅ Servicios detenidos"
else
    log "ℹ️  No hay servicios previos que detener (primera ejecución)"
fi

# 5. START NEW CONTAINERS
log "🚀 Iniciando servicios..."
docker compose up -d 2>&1 | tee -a "$LOG_FILE"
if [ $? -ne 0 ]; then
    error "❌ Error al iniciar los servicios"
fi

# 6. WAIT FOR SERVICES
log "⏳ Esperando que los servicios estén listos (puede tardar 2-3 minutos)..."

wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=60
    local attempt=0

    echo -n "   Esperando $service (puerto $port)... "

    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 3
    done

    echo -e "${RED}❌ TIMEOUT${NC}"
    warning "   $service no respondió a tiempo. Ver logs: docker compose logs $service"
    return 1
}

sleep 15  # Initial wait

wait_for_service "Config Server" 8888
wait_for_service "Discovery Service" 8761
wait_for_service "Auth Service" 8082
wait_for_service "System Service" 8083
wait_for_service "Gateway" 8080

# 7. CLEANUP OLD IMAGES
log "🧹 Limpiando imágenes antiguas..."
docker image prune -f > /dev/null 2>&1
log "✅ Limpieza completada"

# 8. VERIFICAR ESTADO FINAL
log "📊 Estado final de los servicios:"
docker compose ps

# 9. MOSTRAR LOGS RECIENTES SI HAY ERRORES
if ! docker compose ps | grep -q "Up"; then
    warning "⚠️  Algunos servicios no están UP. Mostrando logs..."
    docker compose logs --tail=50
fi

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ DEPLOYMENT COMPLETADO                ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo ""
log "🎉 Deployment completado en commit: $COMMIT_HASH"
log "📍 Endpoints disponibles:"
log "   - Gateway:   http://$(hostname -I | awk '{print $1}'):8080"
log "   - Eureka:    http://$(hostname -I | awk '{print $1}'):8761"
log "   - Auth API:  http://$(hostname -I | awk '{print $1}'):8082"
log "   - System API: http://$(hostname -I | awk '{print $1}'):8083"
echo ""
log "📝 Comandos útiles:"
log "   Ver logs:    docker compose logs -f"
log "   Ver estado:  docker compose ps"
log "   Reiniciar:   docker compose restart <servicio>"
log "   Ver health:  curl http://localhost:8080/actuator/health"
echo ""