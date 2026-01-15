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

# 1. BACKUP
log "📦 Creando backup de la base de datos..."
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

docker exec saas-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} > /dev/null 2>&1
if [ $? -eq 0 ]; then
    docker exec saas-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} saas_db > "$BACKUP_FILE" 2>/dev/null
    log "✅ Backup creado: $BACKUP_FILE"
else
    warning "⚠️  MySQL no está corriendo, saltando backup"
fi

# 2. PULL LATEST CODE
log "📥 Obteniendo últimos cambios del repositorio..."
git fetch origin
git pull origin main || error "Error al hacer pull del repositorio"

COMMIT_HASH=$(git rev-parse --short HEAD)
log "✅ Código actualizado a commit: $COMMIT_HASH"

# 3. VERIFICAR .ENV
if [ ! -f .env ]; then
    error "❌ Archivo .env no encontrado"
fi
source .env

# 4. BUILD IMAGES
log "🏗️  Construyendo imágenes Docker..."
docker-compose build --pull 2>&1 | tee -a "$LOG_FILE"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    error "❌ Error al construir las imágenes"
fi
log "✅ Imágenes construidas exitosamente"

# 5. STOP OLD CONTAINERS (excepto MySQL)
log "🛑 Deteniendo servicios antiguos..."
docker-compose stop config-server discovery-service auth-service system-service gateway-service
log "✅ Servicios detenidos"

# 6. START NEW CONTAINERS
log "🚀 Iniciando nuevos servicios..."
docker-compose up -d 2>&1 | tee -a "$LOG_FILE"
if [ $? -ne 0 ]; then
    error "❌ Error al iniciar los servicios"
fi

# 7. WAIT FOR SERVICES
log "⏳ Esperando que los servicios estén listos..."

wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=0

    echo -n "   Esperando $service (puerto $port)... "

    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    echo -e "${RED}❌ TIMEOUT${NC}"
    return 1
}

sleep 10  # Initial wait

wait_for_service "Config Server" 8888
wait_for_service "Discovery Service" 8761
wait_for_service "Auth Service" 8082
wait_for_service "System Service" 8083
wait_for_service "Gateway" 8080

# 8. CLEANUP OLD IMAGES
log "🧹 Limpiando imágenes antiguas..."
docker image prune -f > /dev/null 2>&1
log "✅ Limpieza completada"

# 9. VERIFICAR ESTADO FINAL
log "📊 Estado final de los servicios:"
docker-compose ps

# 10. MOSTRAR LOGS RECIENTES
log "📝 Últimos logs del Gateway:"
docker-compose logs --tail=20 gateway-service

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ DEPLOYMENT COMPLETADO EXITOSAMENTE   ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo ""
log "🎉 Deployment completado en commit: $COMMIT_HASH"
log "📍 Endpoints disponibles:"
log "   - Gateway: http://$(hostname -I | awk '{print $1}'):8080"
log "   - Eureka: http://$(hostname -I | awk '{print $1}'):8761"
echo ""
log "📝 Ver logs: docker-compose logs -f"
log "📊 Ver estado: docker-compose ps"
echo ""