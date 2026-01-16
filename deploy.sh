#!/bin/bash

# ============================================
# SCRIPT DE DEPLOYMENT AUTOMÁTICO
# Con limpieza forzada de imágenes y verificación mejorada
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuración
REPO_DIR="/opt/saas-platform"
BACKUP_DIR="/opt/saas-backups"
LOG_FILE="/var/log/saas-deploy.log"

# Funciones mejoradas
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ ERROR:${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️${NC}  $1" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️${NC}  $1" | tee -a "$LOG_FILE"
}

# Función mejorada para verificar healthcheck de contenedores
check_container_health() {
    local container_name=$1
    local max_attempts=${2:-60}
    local attempt=1

    info "Verificando salud de ${container_name}..."

    while [ $attempt -le $max_attempts ]; do
        # Verificar si el contenedor existe
        if ! docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            error "Contenedor ${container_name} no existe"
            return 1
        fi

        # Obtener el estado del contenedor
        local container_status=$(docker inspect --format='{{.State.Status}}' ${container_name} 2>/dev/null || echo "not_found")

        # Si el contenedor se detuvo, mostrar logs y fallar
        if [ "$container_status" = "exited" ]; then
            error "Contenedor ${container_name} se detuvo inesperadamente"
            warning "Últimos 50 logs:"
            docker logs --tail 50 ${container_name} | tee -a "$LOG_FILE"
            return 1
        fi

        # Verificar health check
        local health_status=$(docker inspect --format='{{.State.Health.Status}}' ${container_name} 2>/dev/null || echo "none")

        if [ "$health_status" = "healthy" ]; then
            log "✅ ${container_name} está healthy"
            return 0
        elif [ "$health_status" = "none" ]; then
            # Si no hay healthcheck, verificar que esté running
            if [ "$container_status" = "running" ]; then
                log "✅ ${container_name} está running (sin healthcheck)"
                return 0
            fi
        fi

        # Mostrar progreso cada 10 intentos
        if [ $((attempt % 10)) -eq 0 ]; then
            info "   Intento $attempt/$max_attempts - ${container_name}: ${health_status:-$container_status}"
        fi

        sleep 5
        attempt=$((attempt + 1))
    done

    error "Timeout esperando a ${container_name}. Estado: ${health_status:-$container_status}"
    warning "Mostrando últimos 30 logs:"
    docker logs --tail 30 ${container_name} | tee -a "$LOG_FILE"
    return 1
}

# Función para verificar endpoint HTTP
check_http_endpoint() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1

    info "Verificando endpoint HTTP: ${service_name} -> ${url}"

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${url}" > /dev/null 2>&1; then
            log "✅ ${service_name} respondiendo en ${url}"
            return 0
        fi

        if [ $((attempt % 5)) -eq 0 ]; then
            info "   Intento $attempt/$max_attempts - esperando respuesta de ${service_name}..."
        fi

        sleep 2
        attempt=$((attempt + 1))
    done

    warning "${service_name} no respondió en ${url} después de ${max_attempts} intentos"
    return 1
}

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🚀 SAAS PLATFORM DEPLOYMENT SCRIPT      ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Verificar que Docker está corriendo
if ! docker info > /dev/null 2>&1; then
    error "Docker no está corriendo. Inicia Docker primero."
fi

# Verificar directorio
cd "$REPO_DIR" || error "No se puede acceder a $REPO_DIR"

# Verificar .env
[ ! -f .env ] && error "Archivo .env no encontrado en $REPO_DIR"

# Cargar variables de entorno
info "Cargando variables de entorno desde .env"
set -a
source .env
set +a

# Verificar variables críticas
[ -z "$MYSQL_ROOT_PASSWORD" ] && error "MYSQL_ROOT_PASSWORD no está definido en .env"
[ -z "$MYSQL_DATABASE" ] && error "MYSQL_DATABASE no está definido en .env"

log "Configuración cargada correctamente"

# ===========================================
# 1. BACKUP DE MYSQL
# ===========================================
log "📦 Verificando si MySQL necesita backup..."
mkdir -p "$BACKUP_DIR"

if docker ps 2>/dev/null | grep -q saas-mysql; then
    info "MySQL detectado, creando backup..."
    BACKUP_FILE="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

    if docker exec saas-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} >/dev/null 2>&1; then
        if docker exec saas-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > "$BACKUP_FILE" 2>/dev/null; then
            gzip "$BACKUP_FILE"
            log "✅ Backup creado: ${BACKUP_FILE}.gz"

            # Mantener solo los últimos 5 backups
            cd "$BACKUP_DIR"
            ls -t mysql-backup-*.sql.gz 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
        else
            warning "Backup de MySQL falló, continuando de todas formas..."
        fi
    else
        warning "MySQL no responde, saltando backup"
    fi
else
    info "Primera ejecución detectada, no hay MySQL para backup"
fi

# ===========================================
# 2. ACTUALIZAR CÓDIGO
# ===========================================
log "📥 Obteniendo cambios del repositorio..."
git fetch origin 2>&1 | tee -a "$LOG_FILE" || error "git fetch falló"
git reset --hard origin/main 2>&1 | tee -a "$LOG_FILE" || error "No se pudo sincronizar con main"

COMMIT_HASH=$(git rev-parse --short HEAD)
COMMIT_MSG=$(git log -1 --pretty=%B | head -n 1)
log "✅ Código actualizado a commit: $COMMIT_HASH"
info "   Mensaje: $COMMIT_MSG"

# ===========================================
# 3. DETENER Y LIMPIAR CONTENEDORES
# ===========================================
log "🛑 Deteniendo todos los servicios..."
docker compose down --remove-orphans 2>&1 | tee -a "$LOG_FILE" || warning "Error al detener algunos servicios"

# ===========================================
# 4. LIMPIEZA FORZADA DE IMÁGENES ANTIGUAS
# ===========================================
log "🗑️  Eliminando imágenes antiguas de SAAS..."

OLD_IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep "saas-" | awk '{print $2}')

if [ -n "$OLD_IMAGES" ]; then
    IMAGE_COUNT=$(echo "$OLD_IMAGES" | wc -l)
    info "Encontradas $IMAGE_COUNT imágenes antiguas para eliminar"
    echo "$OLD_IMAGES" | xargs docker rmi -f 2>/dev/null || warning "Algunas imágenes no se pudieron eliminar (puede ser normal si están en uso)"
    log "✅ Limpieza de imágenes completada"
else
    info "No hay imágenes antiguas que eliminar"
fi

# Limpiar imágenes huérfanas y caché
log "🧹 Limpiando imágenes huérfanas y caché..."
docker image prune -f >/dev/null 2>&1
docker builder prune -f >/dev/null 2>&1 || true

# ===========================================
# 5. CONSTRUIR IMÁGENES DESDE CERO
# ===========================================
log "🏗️  Construyendo imágenes DESDE CERO..."
info "   ⏱️  Esto puede tomar 10-15 minutos la primera vez"
info "   💡 Puedes ver el progreso en otra terminal con: docker compose logs -f"

if docker compose build --no-cache --pull 2>&1 | tee -a "$LOG_FILE"; then
    log "✅ Todas las imágenes construidas exitosamente"
else
    error "Falló la construcción de imágenes. Revisa los logs arriba."
fi

# Verificar que las imágenes se crearon
log "📊 Imágenes creadas:"
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep -E "saas-|REPOSITORY" | head -10 | tee -a "$LOG_FILE"

# ===========================================
# 6. INICIAR SERVICIOS
# ===========================================
log "🚀 Iniciando servicios en orden de dependencia..."

if docker compose up -d 2>&1 | tee -a "$LOG_FILE"; then
    log "✅ Servicios iniciados correctamente"
else
    error "Error al iniciar servicios. Verifica docker-compose.yml"
fi

# Pequeña pausa inicial
sleep 5

# ===========================================
# 7. VERIFICAR SERVICIOS EN ORDEN
# ===========================================
log "⏳ Verificando servicios en orden de dependencia..."

# 1. MySQL (30 intentos = 2.5 minutos)
if ! check_container_health "saas-mysql" 30; then
    error "MySQL falló al iniciar. Deployment abortado."
fi

# 2. Config Server (30 intentos = 2.5 minutos)
if ! check_container_health "saas-config-server" 30; then
    error "Config Server falló al iniciar. Deployment abortado."
fi
check_http_endpoint "Config Server" "http://localhost:8888/actuator/health" 20

# 3. Discovery Service (40 intentos = 3.3 minutos)
if ! check_container_health "saas-discovery" 40; then
    error "Discovery Service falló al iniciar. Deployment abortado."
fi
check_http_endpoint "Discovery Service" "http://localhost:8761/actuator/health" 20

# 4. Auth Service (60 intentos = 5 minutos)
if ! check_container_health "saas-auth" 60; then
    error "Auth Service falló al iniciar. Mostrando logs completos:"
    docker logs saas-auth | tee -a "$LOG_FILE"
    error "Deployment abortado."
fi
check_http_endpoint "Auth Service" "http://localhost:8082/actuator/health" 30

# 5. System Service (60 intentos = 5 minutos)
if ! check_container_health "saas-system" 60; then
    error "System Service falló al iniciar. Mostrando logs completos:"
    docker logs saas-system | tee -a "$LOG_FILE"
    error "Deployment abortado."
fi
check_http_endpoint "System Service" "http://localhost:8083/actuator/health" 30

# 6. Gateway Service (40 intentos = 3.3 minutos)
if ! check_container_health "saas-gateway" 40; then
    error "Gateway Service falló al iniciar. Mostrando logs completos:"
    docker logs saas-gateway | tee -a "$LOG_FILE"
    error "Deployment abortado."
fi
check_http_endpoint "Gateway Service" "http://localhost:8080/actuator/health" 30

# ===========================================
# 8. VERIFICACIÓN DE INTEGRACIÓN
# ===========================================
log "🔍 Verificando integración de servicios..."

# Esperar a que los servicios se registren en Eureka
info "Esperando registro en Eureka (15 segundos)..."
sleep 15

# Verificar servicios registrados en Eureka
info "Consultando servicios registrados en Eureka..."
EUREKA_APPS=$(curl -s http://localhost:8761/eureka/apps 2>/dev/null || echo "")

if echo "$EUREKA_APPS" | grep -q "<application>"; then
    SERVICES=$(echo "$EUREKA_APPS" | grep -oP '<name>\K[^<]+' | sort -u)
    log "✅ Servicios registrados en Eureka:"
    echo "$SERVICES" | while read -r service; do
        echo "   - $service" | tee -a "$LOG_FILE"
    done
else
    warning "No se pudieron obtener servicios de Eureka (puede ser temporal)"
fi

# Verificar rutas del Gateway
info "Verificando endpoints del Gateway..."
if curl -sf http://localhost:8080/gateway/services > /dev/null 2>&1; then
    log "✅ Gateway respondiendo en endpoints de diagnóstico"
else
    warning "Gateway no responde en /gateway/services (normal si acabas de agregar este endpoint)"
fi

# ===========================================
# 9. LIMPIEZA FINAL
# ===========================================
log "🧹 Limpieza final del sistema..."
docker image prune -f >/dev/null 2>&1
docker container prune -f >/dev/null 2>&1
docker volume prune -f >/dev/null 2>&1 || true

# ===========================================
# 10. RESUMEN Y VERIFICACIÓN FINAL
# ===========================================
log "📊 Estado final de los servicios:"
echo ""
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | tee -a "$LOG_FILE"
echo ""

# Verificar que todos estén healthy o running
UNHEALTHY=$(docker compose ps --format "{{.Name}} {{.Status}}" | grep -v "Up" || true)
if [ -n "$UNHEALTHY" ]; then
    warning "Algunos servicios no están UP:"
    echo "$UNHEALTHY"
    warning "Logs recientes de servicios problemáticos:"
    docker compose logs --tail=30 2>&1 | tee -a "$LOG_FILE"
fi

# Verificar uso de recursos
log "💾 Uso de recursos Docker:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" | head -10

# ===========================================
# BANNER FINAL
# ===========================================
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              ✅ DEPLOYMENT COMPLETADO                      ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

IP_ADDR=$(hostname -I | awk '{print $1}')

log "🎉 Deployment completado exitosamente en $(date)"
log "📍 Commit desplegado: $COMMIT_HASH - $COMMIT_MSG"
echo ""
log "🌐 ENDPOINTS DISPONIBLES:"
log "   ┌─────────────────────────────────────────────────"
log "   │ 🌐 Gateway (API):      http://${IP_ADDR}:8080"
log "   │ 🔍 Eureka Dashboard:   http://${IP_ADDR}:8761"
log "   │ 🔐 Auth Service:       http://${IP_ADDR}:8082"
log "   │ ⚙️  System Service:     http://${IP_ADDR}:8083"
log "   │ 🔧 Config Server:      http://${IP_ADDR}:8888"
log "   └─────────────────────────────────────────────────"
echo ""
log "📝 COMANDOS ÚTILES:"
log "   ┌─────────────────────────────────────────────────"
log "   │ Ver logs en vivo:     docker compose logs -f [servicio]"
log "   │ Ver estado:           docker compose ps"
log "   │ Reiniciar servicio:   docker compose restart [servicio]"
log "   │ Reiniciar todo:       docker compose restart"
log "   │ Ver logs específicos: docker logs -f saas-gateway"
log "   └─────────────────────────────────────────────────"
echo ""
log "🧪 PRUEBAS RÁPIDAS:"
log "   ┌─────────────────────────────────────────────────"
log "   │ Health Gateway:       curl http://localhost:8080/actuator/health"
log "   │ Health Auth:          curl http://localhost:8082/actuator/health"
log "   │ Servicios Gateway:    curl http://localhost:8080/gateway/services"
log "   │ Rutas públicas:       curl http://localhost:8080/gateway/public-routes"
log "   └─────────────────────────────────────────────────"
echo ""

# ===========================================
# VERIFICACIÓN POST-DEPLOYMENT
# ===========================================
log "🔍 Ejecutando verificación post-deployment..."

# Test 1: Gateway health
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    log "   ✅ Gateway health check: OK"
else
    warning "   ⚠️  Gateway health check: FAILED"
fi

# Test 2: Auth health
if curl -sf http://localhost:8082/actuator/health > /dev/null 2>&1; then
    log "   ✅ Auth Service health check: OK"
else
    warning "   ⚠️  Auth Service health check: FAILED"
fi

# Test 3: System health
if curl -sf http://localhost:8083/actuator/health > /dev/null 2>&1; then
    log "   ✅ System Service health check: OK"
else
    warning "   ⚠️  System Service health check: FAILED"
fi

# Test 4: Eureka
if curl -sf http://localhost:8761/actuator/health > /dev/null 2>&1; then
    log "   ✅ Discovery Service health check: OK"
else
    warning "   ⚠️  Discovery Service health check: FAILED"
fi

echo ""
log "✅ Script de deployment finalizado - $(date)"
log "📄 Log completo guardado en: $LOG_FILE"
echo ""

# Si todo está OK, mostrar mensaje final positivo
ALL_HEALTHY=$(docker compose ps --format "{{.Status}}" | grep -c "Up" || echo "0")
TOTAL_SERVICES=$(docker compose ps --format "{{.Name}}" | wc -l)

if [ "$ALL_HEALTHY" -eq "$TOTAL_SERVICES" ]; then
    echo -e "${GREEN}🎊 TODOS LOS SERVICIOS ESTÁN OPERATIVOS ($ALL_HEALTHY/$TOTAL_SERVICES)${NC}"
else
    echo -e "${YELLOW}⚠️  ADVERTENCIA: Solo $ALL_HEALTHY de $TOTAL_SERVICES servicios están operativos${NC}"
    echo -e "${YELLOW}   Revisa los logs con: docker compose logs${NC}"
fi
echo ""