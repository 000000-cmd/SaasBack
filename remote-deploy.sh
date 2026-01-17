#!/bin/bash

# ============================================
# REMOTE DEPLOYMENT - LINUX/MAC
# Despliega desde tu PC al VPS usando SSH keys
# ============================================
# Uso:
#   ./remote-deploy.sh              # Deploy normal
#   ./remote-deploy.sh --setup-ssh  # Configurar SSH keys
#   ./remote-deploy.sh --status     # Ver estado de servicios
#   ./remote-deploy.sh --logs       # Ver logs en vivo
# ============================================

set -euo pipefail

# ============================================
# CONFIGURACION DE COLORES
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

# ============================================
# ARCHIVOS Y DIRECTORIOS
# ============================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/deploy-config.env"
SSH_KEY_FILE="$HOME/.ssh/saas_vps_key"

# ============================================
# FUNCIONES DE UTILIDAD
# ============================================
log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ERROR:${NC} $1"
}

warning() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] ADVERTENCIA:${NC} $1"
}

info() {
    echo -e "${CYAN}[$(date +'%H:%M:%S')] INFO:${NC} $1"
}

# ============================================
# FUNCION: MOSTRAR BANNER
# ============================================
show_banner() {
    echo -e "${BLUE}"
    cat << 'BANNER'
╔══════════════════════════════════════════════════════════════╗
║   REMOTE DEPLOYMENT - SAAS PLATFORM                          ║
║   SSH Keys - Sin Password                                    ║
╚══════════════════════════════════════════════════════════════╝
BANNER
    echo -e "${NC}"
}

# ============================================
# FUNCION: MOSTRAR AYUDA
# ============================================
show_help() {
    echo "Uso: $0 [opcion]"
    echo ""
    echo "Opciones:"
    echo "  (sin argumentos)   Ejecutar deployment completo"
    echo "  --setup-ssh, -s    Configurar SSH keys para acceso sin password"
    echo "  --status           Ver estado de servicios en el VPS"
    echo "  --logs             Ver logs en vivo de todos los servicios"
    echo "  --logs <servicio>  Ver logs de un servicio especifico"
    echo "  --restart          Reiniciar todos los servicios"
    echo "  --help, -h         Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0                    # Deploy normal"
    echo "  $0 --setup-ssh        # Configurar SSH"
    echo "  $0 --logs gateway     # Ver logs del gateway"
    echo ""
}

# ============================================
# FUNCION: CONFIGURAR SSH
# ============================================
setup_ssh() {
    echo -e "${BLUE}"
    cat << 'BANNER'
╔══════════════════════════════════════════════════════════════╗
║   CONFIGURACION DE SSH KEYS                                  ║
╚══════════════════════════════════════════════════════════════╝
BANNER
    echo -e "${NC}"

    # Cargar configuracion
    if [ ! -f "$CONFIG_FILE" ]; then
        error "Archivo de configuracion no encontrado: $CONFIG_FILE"
        exit 1
    fi

    # shellcheck source=/dev/null
    source "$CONFIG_FILE"

    log "Generando clave SSH..."
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh

    if [ -f "$SSH_KEY_FILE" ]; then
        warning "Ya existe una clave SSH en $SSH_KEY_FILE"
        read -rp "Sobrescribir? (s/n): " overwrite
        if [ "$overwrite" != "s" ]; then
            log "Usando clave existente"
            return 0
        fi
    fi

    ssh-keygen -t ed25519 -f "$SSH_KEY_FILE" -N "" -C "saas-deploy-$(date +%Y%m%d)"
    chmod 600 "$SSH_KEY_FILE"
    log "Clave generada: $SSH_KEY_FILE"

    echo ""
    log "Copiando clave al servidor..."
    echo -e "${YELLOW}Se pedira la PASSWORD del VPS${NC}"
    echo ""

    ssh-copy-id -i "${SSH_KEY_FILE}.pub" -p "${VPS_SSH_PORT:-22}" "${VPS_USER}@${VPS_HOST}" || {
        warning "ssh-copy-id fallo, intentando metodo manual..."
        cat "${SSH_KEY_FILE}.pub" | ssh -p "${VPS_SSH_PORT:-22}" "${VPS_USER}@${VPS_HOST}" \
            "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    }

    log "Clave copiada exitosamente"

    echo ""
    log "Verificando conexion..."
    if ssh -i "$SSH_KEY_FILE" -p "${VPS_SSH_PORT:-22}" -o BatchMode=yes "${VPS_USER}@${VPS_HOST}" "echo 'Conexion OK'"; then
        log "SSH sin password configurado correctamente"
    else
        error "La conexion sin password fallo"
        exit 1
    fi

    # Configurar alias en ~/.ssh/config
    log "Configurando alias SSH..."
    SSH_CONFIG="$HOME/.ssh/config"
    touch "$SSH_CONFIG"
    chmod 600 "$SSH_CONFIG"

    # Remover entrada anterior si existe
    sed -i.bak '/^# SAAS Platform VPS/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true
    sed -i.bak '/^Host saas-vps/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true

    cat >> "$SSH_CONFIG" << SSHCONFIG

# SAAS Platform VPS
Host saas-vps
    HostName ${VPS_HOST}
    User ${VPS_USER}
    Port ${VPS_SSH_PORT:-22}
    IdentityFile ${SSH_KEY_FILE}
    ServerAliveInterval 60
    ServerAliveCountMax 3
    StrictHostKeyChecking no
SSHCONFIG

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}SSH configurado exitosamente!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Ahora puedes:"
    echo "  - Conectarte con: ssh saas-vps"
    echo "  - Desplegar con: ./remote-deploy.sh"
    echo ""
}

# ============================================
# FUNCION: CARGAR CONFIGURACION
# ============================================
load_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        error "Archivo de configuracion no encontrado: $CONFIG_FILE"
        echo ""
        echo "Creando archivo de configuracion por defecto..."

        cat > "$CONFIG_FILE" << 'DEFAULTCONFIG'
# Ver deploy-config.env para configuracion completa
VPS_HOST=tu-servidor.com
VPS_USER=root
VPS_PATH=/opt/saas-platform
VPS_SSH_PORT=22
FORCE_REBUILD=false
SKIP_BACKUP=false
AUTO_PULL=true
GIT_BRANCH=main
DEFAULTCONFIG

        echo "Archivo creado: $CONFIG_FILE"
        echo "Por favor, edita el archivo con la configuracion de tu servidor"
        exit 1
    fi

    # shellcheck source=/dev/null
    source "$CONFIG_FILE"

    # Validar variables criticas
    if [ -z "${VPS_HOST:-}" ]; then
        error "VPS_HOST no esta definido en $CONFIG_FILE"
        exit 1
    fi

    if [ -z "${VPS_USER:-}" ]; then
        error "VPS_USER no esta definido en $CONFIG_FILE"
        exit 1
    fi

    if [ -z "${VPS_PATH:-}" ]; then
        error "VPS_PATH no esta definido en $CONFIG_FILE"
        exit 1
    fi
}

# ============================================
# FUNCION: CONFIGURAR COMANDO SSH
# ============================================
setup_ssh_command() {
    if [ -f "$SSH_KEY_FILE" ]; then
        SSH_CMD="ssh -i $SSH_KEY_FILE -p ${VPS_SSH_PORT:-22} ${VPS_USER}@${VPS_HOST}"
        SCP_CMD="scp -i $SSH_KEY_FILE -P ${VPS_SSH_PORT:-22}"
        info "Usando clave SSH: $SSH_KEY_FILE"
    else
        SSH_CMD="ssh -p ${VPS_SSH_PORT:-22} ${VPS_USER}@${VPS_HOST}"
        SCP_CMD="scp -P ${VPS_SSH_PORT:-22}"
        warning "No se encontro clave SSH, se pedira password"
        echo "Ejecuta './remote-deploy.sh --setup-ssh' para configurar acceso sin password"
        echo ""
    fi
}

# ============================================
# FUNCION: VERIFICAR CONEXION
# ============================================
verify_connection() {
    log "Verificando conexion al VPS..."

    if $SSH_CMD "echo 'OK'" &>/dev/null; then
        log "Conexion verificada"
        return 0
    else
        error "No se pudo conectar al VPS"
        echo ""
        echo "Verifica:"
        echo "  1. Que el servidor este accesible: ping ${VPS_HOST}"
        echo "  2. Que las credenciales sean correctas"
        echo "  3. Que el puerto SSH (${VPS_SSH_PORT:-22}) este abierto"
        echo ""
        echo "Si no tienes SSH keys configuradas, ejecuta:"
        echo "  ./remote-deploy.sh --setup-ssh"
        exit 1
    fi
}

# ============================================
# FUNCION: DETECTAR SERVICIOS
# ============================================
detect_services() {
    log "Detectando servicios habilitados..."

    SERVICE_COUNT=0
    while IFS='=' read -r key value; do
        # Ignorar comentarios y lineas vacias
        [[ $key =~ ^#.*$ ]] && continue
        [[ -z $key ]] && continue

        if [[ $key =~ ^DEPLOY_(.+)$ ]]; then
            value=$(echo "$value" | tr -d ' "' | tr '[:upper:]' '[:lower:]')
            if [ "$value" = "true" ]; then
                SERVICE_COUNT=$((SERVICE_COUNT + 1))
                echo "   [x] $key"
            fi
        fi
    done < "$CONFIG_FILE"

    if [ $SERVICE_COUNT -eq 0 ]; then
        error "No hay servicios habilitados para desplegar"
        exit 1
    fi

    echo ""
    log "Total: $SERVICE_COUNT servicios para desplegar"
}

# ============================================
# FUNCION: SUBIR ARCHIVOS
# ============================================
upload_files() {
    log "Subiendo archivos de configuracion..."

    # Subir deploy-config.env
    info "Subiendo deploy-config.env..."
    $SCP_CMD "$CONFIG_FILE" "${VPS_USER}@${VPS_HOST}:${VPS_PATH}/" || {
        error "Error subiendo deploy-config.env"
        exit 1
    }

    # Subir deploy.sh si existe localmente
    if [ -f "${SCRIPT_DIR}/deploy.sh" ]; then
        info "Subiendo deploy.sh..."
        $SCP_CMD "${SCRIPT_DIR}/deploy.sh" "${VPS_USER}@${VPS_HOST}:${VPS_PATH}/" || {
            error "Error subiendo deploy.sh"
            exit 1
        }
    fi

    log "Archivos subidos exitosamente"
}

# ============================================
# FUNCION: EJECUTAR DEPLOYMENT
# ============================================
execute_deployment() {
    log "Configurando permisos en el VPS..."
    $SSH_CMD "cd ${VPS_PATH} && chmod +x deploy.sh" || {
        error "Error configurando permisos"
        exit 1
    }

    echo ""
    echo -e "${BLUE}======================================================${NC}"
    echo -e "${BLUE}   EJECUTANDO DEPLOYMENT EN VPS                       ${NC}"
    echo -e "${BLUE}======================================================${NC}"
    echo ""

    # Ejecutar el script de deploy en el VPS
    $SSH_CMD "cd ${VPS_PATH} && ./deploy.sh"
    DEPLOY_EXIT=$?

    return $DEPLOY_EXIT
}

# ============================================
# FUNCION: VER ESTADO
# ============================================
show_status() {
    load_config
    setup_ssh_command
    verify_connection

    echo ""
    log "Estado de servicios en ${VPS_HOST}:"
    echo ""

    $SSH_CMD "cd ${VPS_PATH} && docker compose ps"

    echo ""
    log "Servicios registrados en Eureka:"
    $SSH_CMD "curl -s http://localhost:8761/eureka/apps 2>/dev/null | grep -oP '<name>\K[^<]+' | sort -u" || echo "   (Eureka no disponible)"
    echo ""
}

# ============================================
# FUNCION: VER LOGS
# ============================================
show_logs() {
    local service="${1:-}"

    load_config
    setup_ssh_command
    verify_connection

    echo ""
    if [ -n "$service" ]; then
        log "Mostrando logs de: $service"
        $SSH_CMD "cd ${VPS_PATH} && docker compose logs -f $service"
    else
        log "Mostrando logs de todos los servicios (Ctrl+C para salir)"
        $SSH_CMD "cd ${VPS_PATH} && docker compose logs -f"
    fi
}

# ============================================
# FUNCION: REINICIAR SERVICIOS
# ============================================
restart_services() {
    load_config
    setup_ssh_command
    verify_connection

    echo ""
    log "Reiniciando todos los servicios..."
    $SSH_CMD "cd ${VPS_PATH} && docker compose restart"

    echo ""
    log "Servicios reiniciados. Esperando que esten healthy..."
    sleep 10

    show_status
}

# ============================================
# FUNCION: DEPLOYMENT PRINCIPAL
# ============================================
run_deployment() {
    show_banner

    load_config
    setup_ssh_command
    verify_connection
    detect_services

    echo ""
    read -rp "Continuar con el deployment? (s/n): " confirm
    if [ "$confirm" != "s" ]; then
        echo "Deployment cancelado"
        exit 0
    fi

    echo ""
    upload_files

    if execute_deployment; then
        echo ""
        echo -e "${GREEN}======================================================${NC}"
        echo -e "${GREEN}   DEPLOYMENT COMPLETADO EXITOSAMENTE                 ${NC}"
        echo -e "${GREEN}======================================================${NC}"
    else
        echo ""
        echo -e "${YELLOW}======================================================${NC}"
        echo -e "${YELLOW}   DEPLOYMENT COMPLETADO CON ADVERTENCIAS            ${NC}"
        echo -e "${YELLOW}======================================================${NC}"
    fi

    echo ""
    echo "Comandos utiles:"
    echo "  Ver estado:    $0 --status"
    echo "  Ver logs:      $0 --logs"
    echo "  Reiniciar:     $0 --restart"
    echo "  Conectar SSH:  ssh saas-vps"
    echo ""
}

# ============================================
# MAIN: PROCESAR ARGUMENTOS
# ============================================
case "${1:-}" in
    --setup-ssh|-s)
        load_config
        setup_ssh
        ;;
    --status)
        show_status
        ;;
    --logs)
        show_logs "${2:-}"
        ;;
    --restart)
        restart_services
        ;;
    --help|-h)
        show_help
        ;;
    "")
        run_deployment
        ;;
    *)
        error "Opcion desconocida: $1"
        echo ""
        show_help
        exit 1
        ;;
esac