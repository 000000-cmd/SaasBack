#!/bin/bash

# ============================================
# SCRIPT DE DEPLOYMENT AUTOMÁTICO
# SaaS Platform - Línea Base
# ============================================
# Características:
# - Encoding UTF-8 correcto
# - Health checks optimizados
# - Rollback automático en caso de fallo
# - Logging mejorado
# - Validación de dependencias
# ============================================

set -euo pipefail

# ============================================
# CONFIGURACIÓN DE COLORES (UTF-8)
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# ============================================
# CONFIGURACIÓN GENERAL
# ============================================
REPO_DIR="/opt/saas-platform"
BACKUP_DIR="/opt/saas-backups"
LOG_FILE="/var/log/saas-deploy.log"
ROLLBACK_FILE="/tmp/saas-rollback-info"
MAX_RETRIES=3
DEPLOY_START_TIME=$(date +%s)

# ============================================
# FUNCIONES DE LOGGING
# ============================================
log() {
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[${timestamp}]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo -e "${RED}[${timestamp}] ERROR:${NC} $1" | tee -a "$LOG_FILE"
}

warning() {
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo -e "${YELLOW}[${timestamp}] ADVERTENCIA:${NC} $1" | tee -a "$LOG_FILE"
}

info() {
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo -e "${CYAN}[${timestamp}] INFO:${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[${timestamp}] OK:${NC} $1" | tee -a "$LOG_FILE"
}

# ============================================
# FUNCIÓN: BANNER
# ============================================
show_banner() {
    echo -e "${BLUE}"
    cat << 'BANNER'
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║   ███████╗ █████╗  █████╗ ███████╗                          ║
║   ██╔════╝██╔══██╗██╔══██╗██╔════╝                          ║
║   ███████╗███████║███████║███████╗                          ║
║   ╚════██║██╔══██║██╔══██║╚════██║                          ║
║   ███████║██║  ██║██║  ██║███████║                          ║
║   ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝                          ║
║                                                              ║
║   PLATFORM DEPLOYMENT SCRIPT v3.0                            ║
║   Linea Base - Microservicios                                ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
BANNER
    echo -e "${NC}"
}

# ============================================
# FUNCIÓN: VERIFICAR PREREQUISITOS
# ============================================
check_prerequisites() {
    log "Verificando prerequisitos..."

    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        error "Docker no esta instalado"
        exit 1
    fi

    # Verificar Docker Compose
    if ! docker compose version &> /dev/null; then
        error "Docker Compose no esta disponible"
        exit 1
    fi

    # Verificar que Docker está corriendo
    if ! docker info &> /dev/null 2>&1; then
        error "Docker daemon no esta corriendo"
        exit 1
    fi

    # Verificar directorio del proyecto
    if [ ! -d "$REPO_DIR" ]; then
        error "Directorio del proyecto no existe: $REPO_DIR"
        exit 1
    fi

    # Verificar archivo .env
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

    # Cargar .env de forma segura
    set -a
    # shellcheck source=/dev/null
    source .env
    set +a

    # Validar variables críticas
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
# FUNCIÓN: GUARDAR ESTADO PARA ROLLBACK
# ============================================
save_rollback_state() {
    log "Guardando estado actual para posible rollback..."

    local current_commit
    current_commit=$(git rev-parse HEAD 2>/dev/null || echo "unknown")

    # Guardar información de imágenes actuales
    {
        echo "PREVIOUS_COMMIT=$current_commit"
        echo "ROLLBACK_TIMESTAMP=$(date +%Y%m%d-%H%M%S)"
        docker images --format "{{.Repository}}:{{.Tag}}" | grep "saas-" | while read -r img; do
            echo "IMAGE=$img"
        done
    } > "$ROLLBACK_FILE"

    success "Estado guardado para rollback"
}

# ============================================
# FUNCIÓN: ROLLBACK
# ============================================
perform_rollback() {
    error "Iniciando rollback..."

    if [ ! -f "$ROLLBACK_FILE" ]; then
        error "No hay informacion de rollback disponible"
        return 1
    fi

    # shellcheck source=/dev/null
    source "$ROLLBACK_FILE"

    if [ -n "${PREVIOUS_COMMIT:-}" ] && [ "$PREVIOUS_COMMIT" != "unknown" ]; then
        warning "Revirtiendo a commit: $PREVIOUS_COMMIT"
        git checkout "$PREVIOUS_COMMIT" 2>/dev/null || true
    fi

    # Intentar restaurar servicios
    docker compose down --remove-orphans 2>/dev/null || true
    docker compose up -d 2>/dev/null || true

    warning "Rollback completado - Verificar estado de los servicios manualmente"
}

# ============================================
# FUNCIÓN: BACKUP DE MYSQL
# ============================================
backup_mysql() {
    log "Verificando necesidad de backup de MySQL..."

    mkdir -p "$BACKUP_DIR"

    # Verificar si MySQL está corriendo
    if ! docker ps 2>/dev/null | grep -q saas-mysql; then
        info "MySQL no esta corriendo, omitiendo backup"
        return 0
    fi

    # Verificar conexión
    if ! docker exec saas-mysql mysqladmin ping -h localhost -uroot -p"${MYSQL_ROOT_PASSWORD}" &>/dev/null; then
        warning "MySQL no responde, omitiendo backup"
        return 0
    fi

    local backup_file="$BACKUP_DIR/mysql-backup-$(date +%Y%m%d-%H%M%S).sql"

    info "Creando backup de base de datos..."

    if docker exec saas-mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        --all-databases \
        --single-transaction \
        --quick \
        --lock-tables=false > "$backup_file" 2>/dev/null; then

        gzip "$backup_file"
        local size
        size=$(du -h "${backup_file}.gz" | cut -f1)
        success "Backup creado: ${backup_file}.gz (${size})"

        # Limpiar backups antiguos (mantener últimos 5)
        cd "$BACKUP_DIR" || return
        # shellcheck disable=SC2012
        ls -t mysql-backup-*.sql.gz 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true

    else
        warning "No se pudo crear backup, continuando sin backup"
    fi
}

# ============================================
# FUNCIÓN: ACTUALIZAR CÓDIGO
# ============================================
update_code() {
    log "Actualizando codigo fuente..."

    cd "$REPO_DIR" || exit 1

    # Fetch cambios
    if ! git fetch origin 2>&1 | tee -a "$LOG_FILE"; then
        error "Error en git fetch"
        return 1
    fi

    # Reset a main
    if ! git reset --hard origin/main 2>&1 | tee -a "$LOG_FILE"; then
        error "Error sincronizando con origin/main"
        return 1
    fi

    local commit_hash commit_msg
    commit_hash=$(git rev-parse --short HEAD)
    commit_msg=$(git log -1 --pretty=%B | head -n 1)

    success "Codigo actualizado: $commit_hash - $commit_msg"

    # Guardar para mostrar al final
    echo "$commit_hash" > /tmp/saas-deploy-commit
    echo "$commit_msg" >> /tmp/saas-deploy-commit
}

# ============================================
# FUNCIÓN: LIMPIAR IMÁGENES ANTIGUAS
# ============================================
cleanup_old_images() {
    log "Limpiando imagenes antiguas..."

    # Listar imágenes SAAS
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

    # Limpiar imágenes huérfanas y cache de builder
    docker image prune -f &>/dev/null || true
    docker builder prune -f &>/dev/null || true
}

# ============================================
# FUNCIÓN: CONSTRUIR IMÁGENES
# ============================================
build_images() {
    log "Construyendo imagenes Docker..."
    info "Esto puede tomar 10-15 minutos la primera vez"

    cd "$REPO_DIR" || exit 1

    # Determinar si usar cache o no
    local build_args=""
    if [ "${FORCE_REBUILD:-false}" = "true" ]; then
        build_args="--no-cache --pull"
        info "Modo FORCE_REBUILD: construyendo sin cache"
    fi

    # Construir con logging
    # shellcheck disable=SC2086
    if docker compose build $build_args 2>&1 | tee -a "$LOG_FILE"; then
        success "Todas las imagenes construidas exitosamente"
    else
        error "Error construyendo imagenes"
        return 1
    fi

    # Mostrar imágenes creadas
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
        # Verificar si el contenedor existe
        if ! docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            error "Contenedor ${container_name} no existe"
            return 1
        fi

        # Obtener estado del contenedor
        local container_status
        container_status=$(docker inspect --format='{{.State.Status}}' "${container_name}" 2>/dev/null || echo "not_found")

        # Si el contenedor se detuvo, mostrar logs y fallar
        if [ "$container_status" = "exited" ]; then
            error "Contenedor ${container_name} se detuvo inesperadamente"
            warning "Ultimos 30 logs:"
            docker logs --tail 30 "${container_name}" 2>&1 | tee -a "$LOG_FILE"
            return 1
        fi

        # Verificar health check
        local health_status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "${container_name}" 2>/dev/null || echo "none")

        if [ "$health_status" = "healthy" ]; then
            success "${container_name} esta healthy"
            return 0
        elif [ "$health_status" = "none" ]; then
            # Si no hay healthcheck, verificar que esté running
            if [ "$container_status" = "running" ]; then
                success "${container_name} esta running (sin healthcheck)"
                return 0
            fi
        fi

        # Mostrar progreso cada 10 intentos
        if [ $((attempt % 8)) -eq 0 ]; then
            info "Intento $attempt/$max_attempts - ${container_name}: ${health_status:-$container_status}"
        fi

        sleep $check_interval
        attempt=$((attempt + 1))
    done

    error "Timeout esperando a ${container_name}"
    warning "Mostrando ultimos 30 logs:"
    docker logs --tail 30 "${container_name}" 2>&1 | tee -a "$LOG_FILE"
    return 1
}

# ============================================
# FUNCIÓN: VERIFICAR ENDPOINT HTTP
# ============================================
check_http_endpoint() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-20}
    local attempt=1

    info "Verificando endpoint HTTP: ${service_name}"

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${url}" &>/dev/null; then
            success "${service_name} respondiendo en ${url}"
            return 0
        fi

        if [ $((attempt % 5)) -eq 0 ]; then
            info "Intento $attempt/$max_attempts esperando ${service_name}..."
        fi

        sleep 2
        attempt=$((attempt + 1))
    done

    warning "${service_name} no respondio despues de ${max_attempts} intentos"
    return 1
}

# ============================================
# FUNCIÓN: INICIAR Y VERIFICAR SERVICIOS
# ============================================
start_and_verify_services() {
    log "Iniciando servicios..."

    cd "$REPO_DIR" || exit 1

    # Detener servicios existentes
    docker compose down --remove-orphans 2>&1 | tee -a "$LOG_FILE" || true

    # Iniciar servicios
    if ! docker compose up -d 2>&1 | tee -a "$LOG_FILE"; then
        error "Error iniciando servicios"
        return 1
    fi

    success "Contenedores iniciados"

    # Espera inicial
    sleep 10

    log "Verificando servicios en orden de dependencia..."

    # 1. MySQL (40 intentos = ~3.3 minutos)
    if ! check_container_health "saas-mysql" 40; then
        error "MySQL fallo al iniciar"
        return 1
    fi

    # 2. Config Server (30 intentos = ~2.5 minutos)
    if ! check_container_health "saas-config-server" 30; then
        error "Config Server fallo al iniciar"
        return 1
    fi
    check_http_endpoint "Config Server" "http://localhost:8888/actuator/health" 15

    # 3. Discovery Service (40 intentos = ~3.3 minutos)
    if ! check_container_health "saas-discovery" 40; then
        error "Discovery Service fallo al iniciar"
        return 1
    fi
    check_http_endpoint "Discovery Service" "http://localhost:8761/actuator/health" 15

    # 4. Auth Service (50 intentos = ~4 minutos)
    if ! check_container_health "saas-auth" 50; then
        error "Auth Service fallo al iniciar"
        docker logs --tail 50 saas-auth 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "Auth Service" "http://localhost:8082/actuator/health" 20

    # 5. System Service (50 intentos = ~4 minutos)
    if ! check_container_health "saas-system" 50; then
        error "System Service fallo al iniciar"
        docker logs --tail 50 saas-system 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "System Service" "http://localhost:8083/actuator/health" 20

    # 6. Gateway Service (40 intentos = ~3.3 minutos)
    if ! check_container_health "saas-gateway" 40; then
        error "Gateway Service fallo al iniciar"
        docker logs --tail 50 saas-gateway 2>&1 | tee -a "$LOG_FILE"
        return 1
    fi
    check_http_endpoint "Gateway Service" "http://localhost:8080/actuator/health" 20

    success "Todos los servicios iniciados correctamente"
}

# ============================================
# FUNCIÓN: VERIFICAR INTEGRACIÓN
# ============================================
verify_integration() {
    log "Verificando integracion de servicios..."

    # Esperar registro en Eureka
    info "Esperando registro en Eureka (20 segundos)..."
    sleep 20

    # Verificar servicios en Eureka
    info "Consultando servicios registrados en Eureka..."
    local eureka_apps
    eureka_apps=$(curl -s http://localhost:8761/eureka/apps 2>/dev/null || echo "")

    if echo "$eureka_apps" | grep -q "<application>"; then
        local services
        services=$(echo "$eureka_apps" | grep -oP '<name>\K[^<]+' | sort -u || true)
        success "Servicios registrados en Eureka:"
        echo "$services" | while read -r service; do
            echo "   - $service" | tee -a "$LOG_FILE"
        done
    else
        warning "No se pudieron obtener servicios de Eureka"
    fi
}

# ============================================
# FUNCIÓN: LIMPIEZA FINAL
# ============================================
final_cleanup() {
    log "Limpieza final del sistema..."

    docker image prune -f &>/dev/null || true
    docker container prune -f &>/dev/null || true

    success "Limpieza completada"
}

# ============================================
# FUNCIÓN: MOSTRAR RESUMEN
# ============================================
show_summary() {
    local deploy_end_time
    deploy_end_time=$(date +%s)
    local duration=$((deploy_end_time - DEPLOY_START_TIME))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))

    # Obtener IP del servidor
    local ip_addr
    ip_addr=$(hostname -I | awk '{print $1}')

    # Obtener información del commit
    local commit_hash="N/A"
    local commit_msg="N/A"
    if [ -f /tmp/saas-deploy-commit ]; then
        commit_hash=$(head -1 /tmp/saas-deploy-commit)
        commit_msg=$(tail -1 /tmp/saas-deploy-commit)
    fi

    echo ""
    echo -e "${GREEN}"
    cat << 'SUMMARY'
╔══════════════════════════════════════════════════════════════╗
║                 DEPLOYMENT COMPLETADO                        ║
╚══════════════════════════════════════════════════════════════╝
SUMMARY
    echo -e "${NC}"

    log "Estado final de los servicios:"
    echo ""
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | tee -a "$LOG_FILE"
    echo ""

    log "Tiempo de despliegue: ${minutes}m ${seconds}s"
    log "Commit desplegado: $commit_hash - $commit_msg"
    echo ""

    echo -e "${CYAN}ENDPOINTS DISPONIBLES:${NC}"
    echo "   ┌─────────────────────────────────────────────────────────"
    echo "   │ Gateway (API):      http://${ip_addr}:8080"
    echo "   │ Eureka Dashboard:   http://${ip_addr}:8761"
    echo "   │ Auth Service:       http://${ip_addr}:8082"
    echo "   │ System Service:     http://${ip_addr}:8083"
    echo "   │ Config Server:      http://${ip_addr}:8888"
    echo "   └─────────────────────────────────────────────────────────"
    echo ""

    echo -e "${CYAN}COMANDOS UTILES:${NC}"
    echo "   ┌─────────────────────────────────────────────────────────"
    echo "   │ Ver logs:           docker compose logs -f [servicio]"
    echo "   │ Ver estado:         docker compose ps"
    echo "   │ Reiniciar:          docker compose restart [servicio]"
    echo "   │ Ver logs Gateway:   docker logs -f saas-gateway"
    echo "   └─────────────────────────────────────────────────────────"
    echo ""

    echo -e "${CYAN}PRUEBAS RAPIDAS:${NC}"
    echo "   ┌─────────────────────────────────────────────────────────"
    echo "   │ curl http://localhost:8080/actuator/health"
    echo "   │ curl http://localhost:8082/actuator/health"
    echo "   │ curl http://localhost:8083/actuator/health"
    echo "   └─────────────────────────────────────────────────────────"
    echo ""

    # Verificar estado final
    local all_healthy
    all_healthy=$(docker compose ps --format "{{.Status}}" | grep -c "Up" || echo "0")
    local total_services
    total_services=$(docker compose ps --format "{{.Name}}" | wc -l)

    if [ "$all_healthy" -eq "$total_services" ]; then
        echo -e "${GREEN}TODOS LOS SERVICIOS OPERATIVOS ($all_healthy/$total_services)${NC}"
    else
        echo -e "${YELLOW}ADVERTENCIA: Solo $all_healthy de $total_services servicios estan operativos${NC}"
        echo -e "${YELLOW}Revisa los logs con: docker compose logs${NC}"
    fi
    echo ""

    log "Script de deployment finalizado - $(date)"
    log "Log completo en: $LOG_FILE"
}

# ============================================
# FUNCIÓN: EJECUTAR POST-DEPLOY TESTS
# ============================================
run_post_deploy_tests() {
    log "Ejecutando verificacion post-deployment..."

    local tests_passed=0
    local tests_failed=0

    # Test 1: Gateway health
    if curl -sf http://localhost:8080/actuator/health &>/dev/null; then
        success "Gateway health check: OK"
        ((tests_passed++))
    else
        warning "Gateway health check: FAILED"
        ((tests_failed++))
    fi

    # Test 2: Auth health
    if curl -sf http://localhost:8082/actuator/health &>/dev/null; then
        success "Auth Service health check: OK"
        ((tests_passed++))
    else
        warning "Auth Service health check: FAILED"
        ((tests_failed++))
    fi

    # Test 3: System health
    if curl -sf http://localhost:8083/actuator/health &>/dev/null; then
        success "System Service health check: OK"
        ((tests_passed++))
    else
        warning "System Service health check: FAILED"
        ((tests_failed++))
    fi

    # Test 4: Eureka
    if curl -sf http://localhost:8761/actuator/health &>/dev/null; then
        success "Discovery Service health check: OK"
        ((tests_passed++))
    else
        warning "Discovery Service health check: FAILED"
        ((tests_failed++))
    fi

    log "Resultado: $tests_passed tests pasaron, $tests_failed fallaron"

    return $tests_failed
}

# ============================================
# FUNCIÓN PRINCIPAL
# ============================================
main() {
    # Crear directorio de logs si no existe
    mkdir -p "$(dirname "$LOG_FILE")"
    mkdir -p "$BACKUP_DIR"

    show_banner

    log "Iniciando deployment - $(date)"
    log "=========================================="

    # Ejecutar pasos del deployment
    check_prerequisites
    load_environment
    save_rollback_state

    # Backup (si está configurado)
    if [ "${SKIP_BACKUP:-false}" != "true" ]; then
        backup_mysql
    else
        info "Backup omitido (SKIP_BACKUP=true)"
    fi

    # Actualizar código (si está configurado)
    if [ "${AUTO_PULL:-true}" = "true" ]; then
        update_code
    else
        info "Actualizacion de codigo omitida (AUTO_PULL=false)"
    fi

    # Detener servicios
    log "Deteniendo servicios existentes..."
    docker compose down --remove-orphans 2>&1 | tee -a "$LOG_FILE" || true

    # Limpiar y construir
    cleanup_old_images

    if ! build_images; then
        error "Fallo la construccion de imagenes"
        perform_rollback
        exit 1
    fi

    # Iniciar y verificar
    if ! start_and_verify_services; then
        error "Fallo el inicio de servicios"
        perform_rollback
        exit 1
    fi

    # Verificar integración
    verify_integration

    # Limpieza final
    final_cleanup

    # Tests post-deployment
    if ! run_post_deploy_tests; then
        warning "Algunos tests post-deployment fallaron"
    fi

    # Mostrar resumen
    show_summary

    # Limpiar archivo temporal
    rm -f /tmp/saas-deploy-commit

    success "Deployment completado exitosamente"
}

# ============================================
# MANEJO DE SEÑALES
# ============================================
cleanup_on_exit() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        error "Script terminado con codigo de error: $exit_code"
        warning "Considerar ejecutar rollback manual si es necesario"
    fi
}

trap cleanup_on_exit EXIT

# ============================================
# EJECUTAR
# ============================================
main "$@"