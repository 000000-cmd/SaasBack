#!/bin/bash
# ============================================
# REMOTE DEPLOY SCRIPT - SAAS PLATFORM
# Ejecutar desde PC local para desplegar en servidor
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================
# CONFIGURACIÓN - MODIFICAR SEGÚN TU SERVIDOR
# ============================================
REMOTE_HOST="${REMOTE_HOST:-tu-servidor.com}"
REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_DIR="${REMOTE_DIR:-/opt/saas-platform}"
SSH_KEY="${SSH_KEY:-}"

# Cargar configuración desde archivo si existe
CONFIG_FILE="deploy-config.env"
if [ -f "$CONFIG_FILE" ]; then
    # shellcheck source=/dev/null
    source "$CONFIG_FILE"
fi

# ============================================
# FUNCIONES DE UTILIDAD
# ============================================
log() {
    echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
    exit 1
}

warning() {
    echo -e "${YELLOW}!${NC} $1"
}

info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# ============================================
# FUNCIÓN: SSH COMMAND
# ============================================
ssh_cmd() {
    local ssh_opts="-o StrictHostKeyChecking=no -o ConnectTimeout=10"

    if [ -n "$SSH_KEY" ] && [ -f "$SSH_KEY" ]; then
        ssh_opts="$ssh_opts -i $SSH_KEY"
    fi

    # shellcheck disable=SC2086
    ssh $ssh_opts -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" "$@"
}

# ============================================
# FUNCIÓN: VERIFICAR CONEXIÓN
# ============================================
check_connection() {
    log "Verificando conexion con servidor..."

    if ! ssh_cmd "echo 'Conexion OK'" &>/dev/null; then
        error "No se puede conectar al servidor $REMOTE_HOST"
    fi

    success "Conexion establecida con $REMOTE_HOST"
}

# ============================================
# FUNCIÓN: VERIFICAR SERVIDOR
# ============================================
check_server() {
    log "Verificando servidor..."

    ssh_cmd "docker --version" &>/dev/null || error "Docker no instalado en servidor"
    ssh_cmd "docker compose version" &>/dev/null || error "Docker Compose no disponible"
    ssh_cmd "[ -d $REMOTE_DIR ]" || error "Directorio $REMOTE_DIR no existe"
    ssh_cmd "[ -f $REMOTE_DIR/.env ]" || error "Archivo .env no encontrado"

    success "Servidor verificado correctamente"
}

# ============================================
# FUNCIÓN: DEPLOY COMPLETO
# ============================================
full_deploy() {
    log "Iniciando FULL DEPLOY en $REMOTE_HOST..."

    check_connection
    check_server

    info "Ejecutando deployment completo..."
    echo ""

    ssh_cmd "cd $REMOTE_DIR && \
        git fetch origin && \
        git reset --hard origin/main && \
        git clean -fd && \
        docker compose down --rmi all --volumes --remove-orphans && \
        docker system prune -f && \
        docker compose build --no-cache && \
        docker compose up -d"

    echo ""
    log "Esperando que los servicios inicien..."
    sleep 30

    show_status

    success "FULL DEPLOY completado!"
}

# ============================================
# FUNCIÓN: QUICK DEPLOY
# ============================================
quick_deploy() {
    log "Iniciando QUICK DEPLOY en $REMOTE_HOST..."

    check_connection
    check_server

    info "Actualizando codigo y reiniciando..."
    echo ""

    ssh_cmd "cd $REMOTE_DIR && \
        git fetch origin && \
        git reset --hard origin/main && \
        git clean -fd && \
        docker compose down && \
        docker compose up -d"

    echo ""
    log "Esperando que los servicios inicien..."
    sleep 20

    show_status

    success "QUICK DEPLOY completado!"
}

# ============================================
# FUNCIÓN: REBUILD SERVICE
# ============================================
rebuild_service() {
    local service=$1

    if [ -z "$service" ]; then
        error "Especifica un servicio: auth-service, system-service, gateway-service"
    fi

    log "Reconstruyendo $service en $REMOTE_HOST..."

    check_connection

    ssh_cmd "cd $REMOTE_DIR && \
        git fetch origin && \
        git reset --hard origin/main && \
        docker compose build --no-cache $service && \
        docker compose up -d $service"

    success "Servicio $service reconstruido"
}

# ============================================
# FUNCIÓN: MOSTRAR ESTADO
# ============================================
show_status() {
    log "Estado de los servicios:"
    echo ""

    ssh_cmd "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'saas-|NAMES'"

    echo ""
    info "URLs de acceso:"
    echo "  - Gateway:   http://$REMOTE_HOST:8080"
    echo "  - Eureka:    http://$REMOTE_HOST:8761"
    echo "  - Config:    http://$REMOTE_HOST:8888"
    echo ""
}

# ============================================
# FUNCIÓN: VER LOGS
# ============================================
show_logs() {
    local service=${1:-}

    log "Mostrando logs..."

    if [ -n "$service" ]; then
        ssh_cmd "cd $REMOTE_DIR && docker compose logs -f --tail=100 $service"
    else
        ssh_cmd "cd $REMOTE_DIR && docker compose logs -f --tail=50"
    fi
}

# ============================================
# FUNCIÓN: STOP
# ============================================
stop_services() {
    log "Deteniendo servicios..."

    check_connection
    ssh_cmd "cd $REMOTE_DIR && docker compose down"

    success "Servicios detenidos"
}

# ============================================
# FUNCIÓN: RESTART
# ============================================
restart_services() {
    local service=${1:-}

    log "Reiniciando servicios..."

    check_connection

    if [ -n "$service" ]; then
        ssh_cmd "cd $REMOTE_DIR && docker compose restart $service"
    else
        ssh_cmd "cd $REMOTE_DIR && docker compose restart"
    fi

    success "Servicios reiniciados"
}

# ============================================
# FUNCIÓN: SETUP SSH KEY
# ============================================
setup_ssh() {
    log "Configurando SSH key..."

    if [ ! -f ~/.ssh/id_rsa ]; then
        info "Generando nueva SSH key..."
        ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""
    fi

    info "Copiando SSH key al servidor..."
    info "Se te pedira la contraseña del servidor:"

    ssh-copy-id -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}"

    success "SSH key configurada"
}

# ============================================
# FUNCIÓN: HELP
# ============================================
show_help() {
    echo ""
    echo -e "${CYAN}SAAS Platform - Remote Deploy${NC}"
    echo ""
    echo "Uso: $0 <comando> [opciones]"
    echo ""
    echo "Comandos:"
    echo "  full              - Deploy completo (pull + rebuild + start)"
    echo "  quick             - Quick deploy (pull + restart sin rebuild)"
    echo "  rebuild <service> - Reconstruir un servicio específico"
    echo "  status            - Ver estado de los servicios"
    echo "  logs [service]    - Ver logs (todos o de un servicio)"
    echo "  stop              - Detener todos los servicios"
    echo "  restart [service] - Reiniciar servicios"
    echo "  setup-ssh         - Configurar SSH key para conexión sin password"
    echo ""
    echo "Configuración:"
    echo "  Crea un archivo 'deploy-config.env' con:"
    echo "    REMOTE_HOST=tu-servidor.com"
    echo "    REMOTE_USER=root"
    echo "    REMOTE_PORT=22"
    echo "    REMOTE_DIR=/opt/saas-platform"
    echo ""
    echo "  O usa variables de entorno:"
    echo "    REMOTE_HOST=servidor.com $0 full"
    echo ""
}

# ============================================
# MAIN
# ============================================
case "${1:-help}" in
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
        check_connection
        show_status
        ;;
    logs)
        check_connection
        show_logs "$2"
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services "$2"
        ;;
    setup-ssh|--setup-ssh)
        setup_ssh
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        error "Comando desconocido: $1. Usa '$0 help' para ver opciones."
        ;;
esac