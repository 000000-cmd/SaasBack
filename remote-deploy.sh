#!/bin/bash

# ============================================
# SCRIPT DE DESPLIEGUE REMOTO DINÁMICO
# Soporta N microservicios sin modificar código
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🚀 DESPLIEGUE REMOTO DINÁMICO           ║
║   Escala automáticamente con tus micros   ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# ============================================
# CARGAR Y VALIDAR CONFIGURACIÓN
# ============================================

CONFIG_FILE="deploy-config.env"

if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}❌ No se encontró $CONFIG_FILE${NC}"
    echo -e "${YELLOW}📝 Creando archivo de configuración por defecto...${NC}"

    cat > "$CONFIG_FILE" << 'CONFIGEOF'
# ============================================
# CONFIGURACIÓN DE DESPLIEGUE DINÁMICO
# ============================================

# Servidor VPS
VPS_HOST=72.62.174.193
VPS_USER=root
VPS_PATH=/opt/saas-platform

# ============================================
# INFRAESTRUCTURA BASE
# ============================================
DEPLOY_MYSQL=true

DEPLOY_CONFIG_SERVER=true
SERVICE_PORT_CONFIG_SERVER=8888
SERVICE_DEPS_CONFIG_SERVER=""

DEPLOY_DISCOVERY_SERVICE=true
SERVICE_PORT_DISCOVERY_SERVICE=8761
SERVICE_DEPS_DISCOVERY_SERVICE="config-server"

# ============================================
# MICROSERVICIOS
# ============================================
DEPLOY_AUTH_SERVICE=true
SERVICE_PORT_AUTH_SERVICE=8082
SERVICE_DEPS_AUTH_SERVICE="mysql config-server discovery-service"

DEPLOY_SYSTEM_SERVICE=true
SERVICE_PORT_SYSTEM_SERVICE=8083
SERVICE_DEPS_SYSTEM_SERVICE="mysql config-server discovery-service"

DEPLOY_GATEWAY_SERVICE=true
SERVICE_PORT_GATEWAY_SERVICE=8080
SERVICE_DEPS_GATEWAY_SERVICE="config-server discovery-service auth-service system-service"

# ============================================
# OPCIONES
# ============================================
FORCE_REBUILD=false
SKIP_BACKUP=false
AUTO_PULL=true
GIT_BRANCH=main
SHOW_LOGS=true
HEALTH_CHECK_TIMEOUT=120
HEALTH_CHECK_INTERVAL=3
CONFIGEOF

    echo -e "${GREEN}✅ Archivo $CONFIG_FILE creado${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📋 Configuración por defecto:${NC}"
    cat "$CONFIG_FILE"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  Edita $CONFIG_FILE antes de continuar${NC}"
    echo -e "${YELLOW}💡 Para agregar un nuevo micro, solo agrega 3 líneas:${NC}"
    echo -e "   DEPLOY_MI_NUEVO_SERVICE=true"
    echo -e "   SERVICE_PORT_MI_NUEVO_SERVICE=8089"
    echo -e "   SERVICE_DEPS_MI_NUEVO_SERVICE=\"mysql config-server discovery-service\""
    echo ""
    exit 0
fi

# Cargar configuración
source "$CONFIG_FILE"

# Validar variables requeridas
if [ -z "$VPS_HOST" ] || [ -z "$VPS_USER" ] || [ -z "$VPS_PATH" ]; then
    echo -e "${RED}❌ Variables VPS_HOST, VPS_USER o VPS_PATH no configuradas${NC}"
    exit 1
fi

# ============================================
# DETECTAR SERVICIOS HABILITADOS
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📋 Analizando servicios a desplegar...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

declare -a ENABLED_SERVICES
declare -A SERVICE_INFO

# Parsear servicios habilitados
while IFS='=' read -r key value; do
    [[ $key =~ ^#.*$ ]] && continue
    [[ -z $key ]] && continue

    if [[ $key =~ ^DEPLOY_(.+)$ ]]; then
        service_name="${BASH_REMATCH[1]}"
        value=$(echo "$value" | tr -d ' "' | tr '[:upper:]' '[:lower:]')

        if [ "$value" = "true" ]; then
            docker_service=$(echo "$service_name" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
            ENABLED_SERVICES+=("$docker_service")

            # Obtener puerto y dependencias
            port_var="SERVICE_PORT_${service_name}"
            deps_var="SERVICE_DEPS_${service_name}"

            port="${!port_var:-N/A}"
            deps="${!deps_var:-none}"

            SERVICE_INFO["${docker_service}_port"]="$port"
            SERVICE_INFO["${docker_service}_deps"]="$deps"

            # Mostrar info del servicio
            printf "   ${GREEN}✓${NC} %-25s Puerto: %-6s Deps: %s\n" \
                "$docker_service" "$port" "$deps"
        fi
    fi
done < "$CONFIG_FILE"

if [ ${#ENABLED_SERVICES[@]} -eq 0 ]; then
    echo -e "${RED}❌ No hay servicios habilitados para desplegar${NC}"
    echo -e "${YELLOW}💡 Edita $CONFIG_FILE y cambia DEPLOY_*=true${NC}"
    exit 1
fi

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}📊 Resumen:${NC}"
echo "   🖥️  Servidor: $VPS_USER@$VPS_HOST"
echo "   📁 Path: $VPS_PATH"
echo "   📦 Servicios: ${#ENABLED_SERVICES[@]} habilitados"
echo "   🌿 Branch: $GIT_BRANCH"
echo "   🔨 Rebuild: $FORCE_REBUILD"
echo "   💾 Backup: $([ "$SKIP_BACKUP" = "true" ] && echo "NO" || echo "SI")"
echo ""

# ============================================
# CONFIRMAR DESPLIEGUE
# ============================================

echo -e "${YELLOW}¿Continuar con el despliegue? (s/n):${NC}"
read -p "> " CONFIRM

if [ "$CONFIRM" != "s" ]; then
    echo "❌ Despliegue cancelado"
    exit 0
fi

# ============================================
# CONFIGURAR SSH
# ============================================

echo ""
echo -e "${GREEN}🔐 Configurando conexión SSH...${NC}"

SSH_KEY_FILE="$HOME/.ssh/saas_vps_key"

if [ ! -f "$SSH_KEY_FILE" ]; then
    echo -e "${YELLOW}⚠️  No se encontró clave SSH${NC}"
    echo -e "${YELLOW}💡 Opciones:${NC}"
    echo "   1. Usar password (menos seguro)"
    echo "   2. Ejecutar ./setup-ssh.sh primero (recomendado)"
    echo ""
    read -p "¿Continuar con password? (s/n): " USE_PASSWORD

    if [ "$USE_PASSWORD" != "s" ]; then
        echo "❌ Despliegue cancelado"
        echo -e "${CYAN}💡 Ejecuta primero: ./setup-ssh.sh${NC}"
        exit 0
    fi

    SSH_CMD="ssh $VPS_USER@$VPS_HOST"
    SCP_CMD="scp"
    echo -e "${YELLOW}⚠️  Se te pedirá la contraseña varias veces${NC}"
else
    SSH_CMD="ssh -i $SSH_KEY_FILE -o StrictHostKeyChecking=no $VPS_USER@$VPS_HOST"
    SCP_CMD="scp -i $SSH_KEY_FILE -o StrictHostKeyChecking=no"
    echo -e "${GREEN}✅ Usando clave SSH${NC}"
fi

# ============================================
# VERIFICAR CONEXIÓN
# ============================================

echo ""
echo -e "${GREEN}1️⃣  Verificando conexión al VPS...${NC}"

if ! $SSH_CMD "echo '✅ Conexión exitosa' 2>&1"; then
    echo -e "${RED}❌ Error de conexión al VPS${NC}"
    echo -e "${YELLOW}💡 Verifica:${NC}"
    echo "   - IP: $VPS_HOST"
    echo "   - Usuario: $VPS_USER"
    echo "   - Conexión a internet"
    echo "   - Firewall/SSH habilitado en VPS"
    exit 1
fi

# ============================================
# SUBIR ARCHIVOS
# ============================================

echo ""
echo -e "${GREEN}2️⃣  Subiendo archivos de configuración...${NC}"

# Crear directorio temporal local
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

# Copiar archivos necesarios
cp "$CONFIG_FILE" "$TMP_DIR/"
cp "deploy-selective.sh" "$TMP_DIR/" 2>/dev/null || {
    echo -e "${RED}❌ No se encontró deploy-selective.sh${NC}"
    exit 1
}

echo "   📤 Subiendo deploy-config.env..."
$SCP_CMD "$TMP_DIR/deploy-config.env" "$VPS_USER@$VPS_HOST:$VPS_PATH/" || {
    echo -e "${RED}❌ Error al subir configuración${NC}"
    exit 1
}

echo "   📤 Subiendo deploy-selective.sh..."
$SCP_CMD "$TMP_DIR/deploy-selective.sh" "$VPS_USER@$VPS_HOST:$VPS_PATH/" || {
    echo -e "${RED}❌ Error al subir script${NC}"
    exit 1
}

echo -e "${GREEN}✅ Archivos subidos${NC}"

# ============================================
# DAR PERMISOS
# ============================================

echo ""
echo -e "${GREEN}3️⃣  Configurando permisos...${NC}"

$SSH_CMD "cd $VPS_PATH && chmod +x deploy-selective.sh" || {
    echo -e "${RED}❌ Error al dar permisos${NC}"
    exit 1
}

echo -e "${GREEN}✅ Permisos configurados${NC}"

# ============================================
# EJECUTAR DESPLIEGUE REMOTO
# ============================================

echo ""
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🚀 EJECUTANDO DESPLIEGUE EN VPS         ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"
echo ""

$SSH_CMD "cd $VPS_PATH && ./deploy-selective.sh"
DEPLOY_EXIT_CODE=$?

# ============================================
# RESULTADO FINAL
# ============================================

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $DEPLOY_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ✅ DESPLIEGUE COMPLETADO EXITOSAMENTE   ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
else
    echo -e "${YELLOW}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   ⚠️  DESPLIEGUE CON ADVERTENCIAS         ║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}💡 Revisa los logs arriba para más detalles${NC}"
fi

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# ============================================
# MOSTRAR LOGS SI ESTÁ HABILITADO
# ============================================

if [ "$SHOW_LOGS" = "true" ] && [ $DEPLOY_EXIT_CODE -eq 0 ]; then
    echo ""
    echo -e "${BLUE}📝 Mostrando logs en tiempo real...${NC}"
    echo -e "${YELLOW}   (Presiona Ctrl+C para salir)${NC}"
    echo ""
    sleep 2

    # Construir lista de servicios para logs
    SERVICES_FOR_LOGS="${ENABLED_SERVICES[*]}"

    $SSH_CMD "cd $VPS_PATH && docker compose logs -f --tail=50 $SERVICES_FOR_LOGS"
fi

# ============================================
# INFORMACIÓN ÚTIL
# ============================================

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📝 Comandos útiles:${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "🔍 Ver logs:"
echo "   ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose logs -f\""
echo ""
echo "📊 Ver estado:"
echo "   ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose ps\""
echo ""
echo "🔄 Reiniciar servicio:"
echo "   ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose restart <servicio>\""
echo ""
echo "🌐 Conectarse al VPS:"
if [ -f "$SSH_KEY_FILE" ]; then
    echo "   ssh -i $SSH_KEY_FILE $VPS_USER@$VPS_HOST"
else
    echo "   ssh $VPS_USER@$VPS_HOST"
fi
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

exit $DEPLOY_EXIT_CODE