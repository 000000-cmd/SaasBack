#!/bin/bash
# ============================================
# DEPLOY SCRIPT - SAAS PLATFORM
# Script de despliegue para servidor
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
LOG_FILE="/var/log/saas-deploy-$(date +%Y%m%d-%H%M%S).log"
ROLLBACK_FILE="/tmp/saas-rollback-state"

# ============================================
# FUNCIONES DE UTILIDAD
# ============================================

log() {
    echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}✓${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}✗${NC} $1" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}!${NC} $1" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${BLUE}ℹ${NC} $1" | tee -a "$LOG_FILE"
}

# ============================================
# FUNCIÓN: VERIFICAR PREREQUISITOS
# ============================================
check_prerequisites() {
    log "Verificando prerequisitos..."

    if ! command -v docker &> /dev/null; then
        error "Docker no esta instalado"
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        error "Docker Compose no esta disponible"
        exit 1
    fi

    if ! docker info &> /dev/null 2>&1; then
        error "Docker daemon no esta corriendo"
        exit 1
    fi

    if [ ! -d "$REPO_DIR" ]; then
        error "Directorio del proyecto no existe: $REPO_DIR"
        exit 1
    fi

    if [ ! -f "$REPO_DIR/.env" ]; then
        error "Archivo .env no encontrado en $REPO_DIR"
        exit 1
    fi

    success "Todos los prerequisitos verificados"
}

# ============================================
# FUNCIÓN: CARGAR VARIABLES DE ENTORNO
# ============================================
load_environment() {
    log "Cargando variables de entorno..."

    cd "$REPO_DIR" || exit 1

    set -a
    # shellcheck source=/dev/null
    source .env
    set +a

    if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
        error "MYSQL_ROOT_PASSWORD no esta definido en .env"
        exit 1
    fi

    if [ -z "${MYSQL_DATABASE:-}" ]; then
        error "MYSQL_DATABASE no esta definido en .env"
        exit 1
    fi

    success "Variables de entorno cargadas"
}

# ============================================
# FUNCIÓN: ACTUALIZAR CÓDIGO
# ============================================
update_code() {
    log "Actualizando codigo desde repositorio..."

    cd "$REPO_DIR" || exit 1

    if ! git fetch origin 2>&1 | tee -a "$LOG_FILE"; then
        error "Error en git fetch"
        return 1
    fi

    if ! git reset --hard origin/main 2>&1 | tee -a "$LOG_FILE"; then
        error "Error sincronizando con origin/main"
        return 1
    fi

    git clean -fd 2>&1 | tee -a "$LOG_FILE"

    local commit_hash commit_msg
    commit_hash=$(git rev-parse --short HEAD)
    commit_msg=$(git log -1 --pretty=%B | head -n 1)

    success "Codigo actualizado: $commit_hash - $commit_msg"

    echo "$commit_hash" > /tmp/saas-deploy-commit
    echo "$commit_msg" >> /tmp/saas-deploy-commit
}

# ============================================
# FUNCIÓN: LIMPIAR IMÁGENES ANTIGUAS
# ============================================
cleanup_old_images() {
    log "Limpiando imagenes antiguas..."

    local old_images
    old_images=$(docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" 2>/dev/null | grep "saas-" | awk '{print $2}' || true)

    if [ -n "$old_images" ]; then
        local count
        count=$(echo "$old_images" | wc -l)
        info "Encontradas $count imagenes SAAS para eliminar"

        echo "$old_images" | xargs -r docker rmi -f 2>/dev/null || true
        success "Imagenes antiguas eliminadas"
    else
        info "No hay imagenes antiguas que eliminar"
    fi

    docker image prune -f &>/dev/null || true
    docker builder prune -f &>/dev/null || true
}

# ============================================
# FUNCIÓN: CONSTRUIR IMÁGENES
# ============================================
build_images() {
    log "Construyendo imagenes Docker..."
    info "Esto puede tomar 5-15 minutos"

    cd "$REPO_DIR" || exit 1

    local build_args=""
    if [ "${FORCE_REBUILD:-false}" = "true" ]; then
        build_args="--no-cache --pull"
        info "Modo FORCE_REBUILD: construyendo sin cache"
    fi

    # shellcheck disable=SC2086
    if docker compose build $build_args 2>&1 | tee -a "$LOG_FILE"; then
        success "Todas las imagenes construidas exitosamente"
    else
        error "Error construyendo imagenes"
        return 1
    fi

    log "Imagenes creadas:"
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep -E "saas-|REPOSITORY" | head -10 | tee -a "$LOG_FILE"
}

# ============================================
# FUNCIÓN: HEALTH CHECK DE CONTENEDOR
# ============================================
check_container_health() {
    local container_name=$1
    local max_attempts=${2:-40}
    local attempt=1
    local check_interval=5

    info "Verificando salud de ${container_name}..."

    while [ $attempt -le $max_attempts ]; do
        if ! docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            warning "Contenedor $container_name no existe (intento $attempt/$max_attempts)"
            sleep $check_interval
            attempt=$((attempt + 1))
            continue
        fi

        local health_status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "unknown")

        case "$health_status" in
            "healthy")
                success "$container_name esta healthy"
                return 0
                ;;
            "unhealthy")
                error "$container_name esta unhealthy"
                docker logs --tail 20 "$container_name" 2>&1 | tee -a "$LOG_FILE"
                return 1
                ;;
            "starting")
                printf "."
                ;;
            *)
                printf "."
                ;;
        esac

        sleep $check_interval
        attempt=$((attempt + 1))
    done

    error "$container_name no alcanzo estado healthy en tiempo esperado"
    docker logs --tail 30 "$container_name" 2>&1 | tee -a "$LOG_FILE"
    return 1
}

# ============================================
# FUNCIÓN: CHECK HTTP ENDPOINT
# ============================================
check_http_endpoint() {
    local name=$1
    local url=$2
    local max_attempts=${3:-10}
    local attempt=1

    info "Verificando endpoint HTTP de $name..."

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            success "$name respondiendo en $url"
            return 0
        fi
        sleep 3
        attempt=$((attempt + 1))
    done

    warning "$name no responde en $url (puede estar iniciando)"
    return 0
}

# ============================================
# FUNCIÓN: INICIAR SERVICIOS
# ============================================
start_services() {
    log "Iniciando servicios..."

    cd "$REPO_DIR" || exit 1

    docker compose down --remove-orphans 2>&1 | tee -a "$LOG_FILE" || true

    if ! docker compose up -d 2>&1 | tee -a "$LOG_FILE"; then
        error "Error iniciando servicios"
        return 1
    fi

    success "Contenedores iniciados"

    sleep 10

    log "Verificando servicios en orden de dependencia..."

    # 1. MySQL
    if ! check_container_health "saas-mysql" 40; then
        error "MySQL fallo al iniciar"
        return 1
    fi

    # 2. Config Server
    if ! check_container_health "saas-config-server" 30; then
        error "Config Server fallo al iniciar"
        return 1
    fi
    check_http_endpoint "Config Server" "http://localhost:8888/actuator/health" 15

    # 3. Discovery Service
    if ! check_container_health "saas-discovery" 40; then
        error "Discovery Service fallo al iniciar"
        return 1
    fi
    check_http_endpoint "Discovery Service" "http://localhost:8761/actuator/health" 15

    # 4. Auth Service
    if ! check_container_health "saas-auth" 50; then
        error "Auth Service fallo al iniciar"
        docker logs --tail 50 saas-auth 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "Auth Service" "http://localhost:8082/actuator/health" 20

    # 5. System Service
    if ! check_container_health "saas-system" 50; then
        error "System Service fallo al iniciar"
        docker logs --tail 50 saas-system 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "System Service" "http://localhost:8083/actuator/health" 20

    # 6. Gateway Service
    if ! check_container_health "saas-gateway" 40; then
        error "Gateway Service fallo al iniciar"
        docker logs --tail 50 saas-gateway 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "Gateway Service" "http://localhost:8080/actuator/health" 20

    success "Todos los servicios iniciados correctamente"
}

# ============================================
# FUNCIÓN: MOSTRAR RESUMEN
# ============================================
show_summary() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}         DEPLOYMENT COMPLETADO${NC}"
    echo -e "${CYAN}============================================${NC}"
    echo ""

    if [ -f /tmp/saas-deploy-commit ]; then
        local commit_info
        commit_info=$(cat /tmp/saas-deploy-commit)
        echo -e "${GREEN}Commit:${NC} $commit_info"
    fi

    echo ""
    echo -e "${GREEN}Servicios desplegados:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "saas-|NAMES"
    echo ""

    echo -e "${GREEN}URLs de acceso:${NC}"
    echo "  - Gateway:   http://localhost:8080"
    echo "  - Eureka:    http://localhost:8761"
    echo "  - Config:    http://localhost:8888"
    echo "  - Auth:      http://localhost:8082"
    echo "  - System:    http://localhost:8083"
    echo ""

    echo -e "${CYAN}Log guardado en:${NC} $LOG_FILE"
    echo ""
}

# ============================================
# FUNCIÓN: DEPLOY COMPLETO
# ============================================
full_deploy() {
    log "Iniciando deployment completo..."

    check_prerequisites
    load_environment
    update_code
    cleanup_old_images
    build_images
    start_services
    show_summary

    success "Deployment completado exitosamente!"
}

# ============================================
# FUNCIÓN: QUICK DEPLOY (sin rebuild)
# ============================================
quick_deploy() {
    log "Iniciando quick deployment..."

    check_prerequisites
    load_environment
    update_code

    cd "$REPO_DIR" || exit 1
    docker compose down --remove-orphans 2>&1 | tee -a "$LOG_FILE" || true
    docker compose up -d 2>&1 | tee -a "$LOG_FILE"

    sleep 15
    show_summary

    success "Quick deployment completado!"
}

# ============================================
# FUNCIÓN: REBUILD ESPECÍFICO
# ============================================
rebuild_service() {
    local service=$1

    if [ -z "$service" ]; then
        error "Especifica un servicio: auth-service, system-service, gateway-service, etc."
        exit 1
    fi

    log "Reconstruyendo servicio: $service"

    check_prerequisites
    load_environment

    cd "$REPO_DIR" || exit 1

    docker compose build --no-cache "$service" 2>&1 | tee -a "$LOG_FILE"
    docker compose up -d "$service" 2>&1 | tee -a "$LOG_FILE"

    success "Servicio $service reconstruido"
}

# ============================================
# MAIN
# ============================================
case "${1:-full}" in
    full)
        full_deploy
        ;;
    quick)
        quick_deploy
        ;;
    rebuild)
        rebuild_service "$2"
        ;;
    status)
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "saas-|NAMES"
        ;;
    logs)
        docker compose logs -f "${2:-}"
        ;;
    stop)
        docker compose down
        ;;
    restart)
        docker compose restart "${2:-}"
        ;;
    *)
        echo "Uso: $0 {full|quick|rebuild <service>|status|logs [service]|stop|restart [service]}"
        echo ""
        echo "Comandos:"
        echo "  full              - Deployment completo (git pull + build + start)"
        echo "  quick             - Quick deploy (git pull + restart, sin rebuild)"
        echo "  rebuild <service> - Reconstruir un servicio específico"
        echo "  status            - Ver estado de los contenedores"
        echo "  logs [service]    - Ver logs (todos o de un servicio)"
        echo "  stop              - Detener todos los servicios"
        echo "  restart [service] - Reiniciar servicios"
        exit 1
        ;;
esac