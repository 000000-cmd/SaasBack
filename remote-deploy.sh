#!/bin/bash

# ============================================
# DESPLIEGUE RÁPIDO - LINUX/MAC (CON PASSWORD)
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
║   🚀 DESPLIEGUE RÁPIDO - LINUX            ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# ============================================
# CARGAR CREDENCIALES
# ============================================

CREDENTIALS_FILE="credentials.conf"

if [ ! -f "$CREDENTIALS_FILE" ]; then
    echo -e "${RED}❌ No se encontró $CREDENTIALS_FILE${NC}"
    echo -e "${YELLOW}📝 Creando archivo de credenciales...${NC}"

    cat > "$CREDENTIALS_FILE" << 'CREDEOF'
# ============================================
# CREDENCIALES VPS
# ============================================
VPS_HOST=72.62.174.193
VPS_USER=root
VPS_PASSWORD=H;v1c-#-b,9DlzMRj;L3
VPS_PATH=/opt/saas-platform
VPS_SSH_PORT=22
CREDEOF

    chmod 600 "$CREDENTIALS_FILE"
    echo -e "${GREEN}✅ Archivo creado: $CREDENTIALS_FILE${NC}"
    echo ""
fi

# Cargar credenciales
source "$CREDENTIALS_FILE"

# Validar que se cargaron
if [ -z "$VPS_HOST" ] || [ -z "$VPS_PASSWORD" ]; then
    echo -e "${RED}❌ Error: No se pudieron cargar las credenciales${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Credenciales cargadas${NC}"
echo "   Host: $VPS_HOST"
echo "   User: $VPS_USER"
echo ""

# ============================================
# VERIFICAR DEPENDENCIAS
# ============================================

# Verificar si sshpass está instalado
if ! command -v sshpass &> /dev/null; then
    echo -e "${YELLOW}⚠️  sshpass no está instalado${NC}"
    echo ""
    echo -e "${CYAN}💡 Para instalar sshpass:${NC}"
    echo ""
    echo "Ubuntu/Debian:"
    echo "  sudo apt-get install sshpass"
    echo ""
    echo "macOS:"
    echo "  brew install hudochenkov/sshpass/sshpass"
    echo ""
    echo "CentOS/RHEL:"
    echo "  sudo yum install sshpass"
    echo ""
    read -p "¿Instalar ahora? (s/n): " INSTALL_SSHPASS

    if [ "$INSTALL_SSHPASS" = "s" ]; then
        if command -v apt-get &> /dev/null; then
            sudo apt-get update && sudo apt-get install -y sshpass
        elif command -v brew &> /dev/null; then
            brew install hudochenkov/sshpass/sshpass
        elif command -v yum &> /dev/null; then
            sudo yum install -y sshpass
        else
            echo -e "${RED}❌ No se pudo instalar sshpass automáticamente${NC}"
            echo "Por favor, instálalo manualmente"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  Sin sshpass, se usará método interactivo (pedirá password)${NC}"
        USE_SSHPASS=false
    fi
else
    echo -e "${GREEN}✅ sshpass encontrado${NC}"
    USE_SSHPASS=true
fi

echo ""

# ============================================
# CARGAR CONFIGURACIÓN DE SERVICIOS
# ============================================

CONFIG_FILE="deploy-config.env"

if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${YELLOW}📝 Creando configuración de servicios por defecto...${NC}"

    cat > "$CONFIG_FILE" << 'CONFIGEOF'
# Servidor VPS (se carga desde credentials.conf)

# Infraestructura
DEPLOY_MYSQL=true
DEPLOY_CONFIG_SERVER=true
SERVICE_PORT_CONFIG_SERVER=8888
DEPLOY_DISCOVERY_SERVICE=true
SERVICE_PORT_DISCOVERY_SERVICE=8761

# Microservicios
DEPLOY_AUTH_SERVICE=true
SERVICE_PORT_AUTH_SERVICE=8082
DEPLOY_SYSTEM_SERVICE=true
SERVICE_PORT_SYSTEM_SERVICE=8083
DEPLOY_GATEWAY_SERVICE=true
SERVICE_PORT_GATEWAY_SERVICE=8080

# Opciones
FORCE_REBUILD=false
SKIP_BACKUP=false
AUTO_PULL=true
GIT_BRANCH=main
SHOW_LOGS=true
CONFIGEOF

    echo -e "${GREEN}✅ Archivo creado: $CONFIG_FILE${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  Edita el archivo si necesitas cambiar servicios${NC}"
    echo ""
    read -p "¿Editar ahora? (s/n): " EDIT_NOW
    if [ "$EDIT_NOW" = "s" ]; then
        ${EDITOR:-nano} "$CONFIG_FILE"
    fi
fi

# Detectar servicios habilitados
echo -e "${GREEN}📋 Detectando servicios habilitados...${NC}"

SERVICE_COUNT=0
while IFS='=' read -r key value; do
    [[ $key =~ ^#.*$ ]] && continue
    [[ -z $key ]] && continue

    if [[ $key =~ ^DEPLOY_(.+)$ ]]; then
        value=$(echo "$value" | tr -d ' "' | tr '[:upper:]' '[:lower:]')
        if [ "$value" = "true" ]; then
            SERVICE_COUNT=$((SERVICE_COUNT + 1))
            echo "   ✓ $key"
        fi
    fi
done < "$CONFIG_FILE"

if [ $SERVICE_COUNT -eq 0 ]; then
    echo -e "${RED}❌ No hay servicios habilitados${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}📊 Total servicios: $SERVICE_COUNT${NC}"
echo ""

# Confirmar
read -p "¿Continuar con el despliegue? (s/n): " CONFIRM
if [ "$CONFIRM" != "s" ]; then
    echo "❌ Cancelado"
    exit 0
fi

# ============================================
# CONFIGURAR COMANDOS SSH/SCP
# ============================================

if [ "$USE_SSHPASS" = "true" ]; then
    SSH_CMD="sshpass -p '$VPS_PASSWORD' ssh -o StrictHostKeyChecking=no -p $VPS_SSH_PORT $VPS_USER@$VPS_HOST"
    SCP_CMD="sshpass -p '$VPS_PASSWORD' scp -o StrictHostKeyChecking=no -P $VPS_SSH_PORT"
else
    SSH_CMD="ssh -o StrictHostKeyChecking=no -p $VPS_SSH_PORT $VPS_USER@$VPS_HOST"
    SCP_CMD="scp -o StrictHostKeyChecking=no -P $VPS_SSH_PORT"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}⚠️  Se te pedirá la PASSWORD del VPS varias veces${NC}"
    echo "   Password: $VPS_PASSWORD"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
fi

# ============================================
# VERIFICAR CONEXIÓN
# ============================================

echo ""
echo -e "${GREEN}1️⃣  Verificando conexión al VPS...${NC}"

eval "$SSH_CMD 'echo OK'" > /dev/null 2>&1 || {
    echo -e "${RED}❌ Error de conexión${NC}"
    echo ""
    echo -e "${YELLOW}💡 Verifica:${NC}"
    echo "   - Host: $VPS_HOST"
    echo "   - User: $VPS_USER"
    echo "   - Password: $VPS_PASSWORD"
    echo "   - Puerto SSH: $VPS_SSH_PORT"
    exit 1
}

echo -e "${GREEN}✅ Conexión exitosa${NC}"

# ============================================
# SUBIR ARCHIVOS
# ============================================

echo ""
echo -e "${GREEN}2️⃣  Subiendo archivos al servidor...${NC}"

echo "   📤 Subiendo credentials.conf..."
eval "$SCP_CMD '$CREDENTIALS_FILE' $VPS_USER@$VPS_HOST:$VPS_PATH/credentials.conf" || {
    echo -e "${RED}❌ Error al subir credentials${NC}"
    exit 1
}

echo "   📤 Subiendo deploy-config.env..."
eval "$SCP_CMD '$CONFIG_FILE' $VPS_USER@$VPS_HOST:$VPS_PATH/deploy-config.env" || {
    echo -e "${RED}❌ Error al subir config${NC}"
    exit 1
}

echo "   📤 Subiendo deploy-selective.sh..."
eval "$SCP_CMD deploy-selective.sh $VPS_USER@$VPS_HOST:$VPS_PATH/deploy-selective.sh" || {
    echo -e "${RED}❌ Error al subir script${NC}"
    exit 1
}

echo -e "${GREEN}✅ Archivos subidos${NC}"

# ============================================
# DAR PERMISOS Y EJECUTAR
# ============================================

echo ""
echo -e "${GREEN}3️⃣  Configurando permisos...${NC}"
eval "$SSH_CMD 'cd $VPS_PATH && chmod +x deploy-selective.sh'"
echo -e "${GREEN}✅ Permisos configurados${NC}"

# ============================================
# EJECUTAR DESPLIEGUE
# ============================================

echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   🚀 EJECUTANDO DESPLIEGUE EN VPS         ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════╝${NC}"
echo ""

eval "$SSH_CMD 'cd $VPS_PATH && ./deploy-selective.sh'"
DEPLOY_EXIT=$?

# ============================================
# RESULTADO
# ============================================

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $DEPLOY_EXIT -eq 0 ]; then
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ✅ DESPLIEGUE COMPLETADO                ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
else
    echo -e "${YELLOW}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   ⚠️  DESPLIEGUE CON ADVERTENCIAS         ║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════╝${NC}"
fi

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}📝 Comandos útiles:${NC}"
echo ""
echo "🔍 Ver logs:"
if [ "$USE_SSHPASS" = "true" ]; then
    echo "   sshpass -p '$VPS_PASSWORD' ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose logs -f\""
else
    echo "   ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose logs -f\""
fi
echo ""
echo "📊 Ver estado:"
if [ "$USE_SSHPASS" = "true" ]; then
    echo "   sshpass -p '$VPS_PASSWORD' ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose ps\""
else
    echo "   ssh $VPS_USER@$VPS_HOST \"cd $VPS_PATH && docker compose ps\""
fi
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

exit $DEPLOY_EXIT