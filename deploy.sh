#!/bin/bash

# ============================================
# SCRIPT DE DEPLOYMENT AUTOMÁTICO
# Con limpieza forzada de imágenes
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# Verificar directorio
cd "$REPO_DIR" || error "No se puede acceder a $REPO_DIR"

# Verificar .env
[ ! -f .env ] && error "❌ Archivo .env no encontrado"

# Cargar variables
set -a
source .env
set +a

# ===========================================
# 1. BACKUP DE MYSQL
# ===========================================
log "📦 Verificando si MySQL necesita backup..."
mkdir -p "$BACKUP_DIR"

if docker ps 2>/dev/null | grep -q saas-mysql; then
    log "   MySQL detectado, creando backup..."
    BACKUP_FILE="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

    if docker exec saas-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} >/dev/null 2>&1; then
        docker exec saas-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > "$BACKUP_FILE" 2>/dev/null || true

        if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
            gzip "$BACKUP_FILE"
            log "   ✅ Backup: ${BACKUP_FILE}.gz"
        else
            warning "   ⚠️  Backup falló, continuando..."
        fi
    else
        warning "   ⚠️  MySQL no responde, saltando backup"
    fi
else
    log "   ℹ️  Primera ejecución, saltando backup"
fi

# ===========================================
# 2. ACTUALIZAR CÓDIGO
# ===========================================
log "📥 Obteniendo cambios del repositorio..."
git fetch origin 2>&1 | tee -a "$LOG_FILE" || error "❌ git fetch falló"
git reset --hard origin/main 2>&1 | tee -a "$LOG_FILE" || error "❌ No se pudo sincronizar"

COMMIT_HASH=$(git rev-parse --short HEAD)
log "✅ Código actualizado a commit: $COMMIT_HASH"

# ===========================================
# 3. DETENER Y LIMPIAR CONTENEDORES
# ===========================================
log "🛑 Deteniendo todos los servicios..."
docker compose down 2>&1 | tee -a "$LOG_FILE" || warning "   ⚠️  Error al detener servicios"

# ===========================================
# 4. LIMPIEZA FORZADA DE IMÁGENES ANTIGUAS
# ===========================================
log "🗑️  Eliminando imágenes antiguas de SAAS..."

# Obtener IDs de imágenes SAAS
OLD_IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep "saas-" | awk '{print $2}')

if [ ! -z "$OLD_IMAGES" ]; then
    log "   Encontradas $(echo "$OLD_IMAGES" | wc -l) imágenes antiguas"
    echo "$OLD_IMAGES" | xargs docker rmi -f 2>/dev/null || warning "   ⚠️  Algunas imágenes no se pudieron eliminar"
    log "   ✅ Imágenes antiguas eliminadas"
else
    log "   ℹ️  No hay imágenes antiguas que eliminar"
fi

# Limpiar imágenes huérfanas
log "🧹 Limpiando imágenes huérfanas..."
docker image prune -f >/dev/null 2>&1

# ===========================================
# 5. CONSTRUIR IMÁGENES DESDE CERO
# ===========================================
log "🏗️  Construyendo imágenes DESDE CERO (10-15 min primera vez)..."
log "   💡 Tip: Ver progreso en otra terminal con: docker compose logs -f"

docker compose build --no-cache --pull 2>&1 | tee -a "$LOG_FILE"

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    error "❌ Error al construir imágenes. Revisa los logs arriba."
fi

log "✅ Imágenes construidas exitosamente"

# Verificar que las imágenes se crearon
log "📊 Imágenes creadas:"
docker images | grep "saas-" | tee -a "$LOG_FILE"

# ===========================================
# 6. INICIAR SERVICIOS
# ===========================================
log "🚀 Iniciando servicios..."
docker compose up -d 2>&1 | tee -a "$LOG_FILE" || error "❌ Error al iniciar servicios"

# ===========================================
# 7. ESPERAR Y VERIFICAR SERVICIOS
# ===========================================
log "⏳ Esperando que los servicios estén listos (2-3 min)..."
sleep 20

wait_for_service() {
    local name=$1
    local port=$2
    local max_attempts=60

    echo -n "   Esperando $name (puerto $port)... "

    for attempt in $(seq 1 $max_attempts); do
        if curl -sf http://localhost:$port/actuator/health >/dev/null 2>&1; then
            echo -e "${GREEN}✅ UP${NC}"
            return 0
        fi
        sleep 3
    done

    echo -e "${YELLOW}⚠️  TIMEOUT${NC}"
    warning "   $name no respondió a tiempo"
    return 1
}

# Verificar servicios en orden
wait_for_service "Config Server" 8888
wait_for_service "Discovery" 8761
wait_for_service "Auth Service" 8082
wait_for_service "System Service" 8083
wait_for_service "Gateway" 8080

# ===========================================
# 8. LIMPIEZA FINAL
# ===========================================
log "🧹 Limpieza final..."
docker image prune -f >/dev/null 2>&1
docker container prune -f >/dev/null 2>&1

# ===========================================
# 9. VERIFICACIÓN FINAL
# ===========================================
log "📊 Estado final de los servicios:"
docker compose ps | tee -a "$LOG_FILE"

# Verificar que todos estén UP
if ! docker compose ps | grep -q "Up"; then
    warning "⚠️  Algunos servicios no están UP. Logs recientes:"
    docker compose logs --tail=30 2>&1 | tee -a "$LOG_FILE"
fi

# ===========================================
# RESUMEN FINAL
# ===========================================
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ DEPLOYMENT COMPLETADO                ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo ""

log "🎉 Deployment completado exitosamente"
log "📍 Commit desplegado: $COMMIT_HASH"
log ""
log "🌐 Endpoints disponibles:"
IP_ADDR=$(hostname -I | awk '{print $1}')
log "   Gateway:    http://${IP_ADDR}:8080"
log "   Eureka:     http://${IP_ADDR}:8761"
log "   Auth API:   http://${IP_ADDR}:8082"
log "   System API: http://${IP_ADDR}:8083"
log "   Config:     http://${IP_ADDR}:8888"
echo ""
log "📝 Comandos útiles:"
log "   Ver logs:        docker compose logs -f [servicio]"
log "   Ver estado:      docker compose ps"
log "   Reiniciar todo:  docker compose restart"
log "   Health Gateway:  curl http://localhost:8080/actuator/health"
log "   Health Auth:     curl http://localhost:8082/actuator/health"
echo ""

# Verificación rápida del gateway
log "🔍 Verificación rápida del Gateway..."
if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    log "   ✅ Gateway respondiendo correctamente"
else
    warning "   ⚠️  Gateway no responde, verifica los logs"
fi

log "✅ Deployment finalizado - $(date)"