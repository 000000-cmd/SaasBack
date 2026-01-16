#!/bin/bash

# ============================================
# REMOTE DEPLOYMENT - LINUX/MAC
# Despliega desde tu PC al VPS usando SSH keys
# ============================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Archivos
CONFIG_FILE="deploy-config.env"
SSH_KEY_FILE="$HOME/.ssh/saas_vps_key"

# ============================================
# FUNCIÓN: SETUP SSH
# ============================================

setup_ssh() {
    echo -e "${BLUE}"
    cat << "EOF"
╔═══════════════════════════════════════════╗
║   🔐 CONFIGURACIÓN DE SSH KEYS            ║
╚═══════════════════════════════════════════╝
EOF
    echo -e "${NC}"

    # Cargar config para obtener credenciales
    if [ ! -f "$CONFIG_FILE" ]; then
        echo -e "${RED}❌ Error: $CONFIG_FILE no encontrado${NC}"
        exit 1
    fi

    source "$CONFIG_FILE"

    echo -e "${GREEN}1️⃣  Generando clave SSH...${NC}"
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh

    if [ -f "$SSH_KEY_FILE" ]; then
        echo -e "${YELLOW}⚠️  Ya existe una clave SSH${NC}"
        read -p "¿Sobrescribir? (s/n): " OVERWRITE
        [ "$OVERWRITE" != "s" ] && echo "✅ Usando clave existente" && return 0
    fi

    ssh-keygen -t ed25519 -f "$SSH_KEY_FILE" -N "" -C "saas-deploy-$(date +%Y%m%d)"
    chmod 600 "$SSH_KEY_FILE"
    echo -e "${GREEN}✅ Clave generada: $SSH_KEY_FILE${NC}"

    echo ""
    echo -e "${GREEN}2️⃣  Copiando clave al servidor...${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}⚠️  Se pedirá la PASSWORD del VPS${NC}"
    echo -e "${YELLOW}   Password: $VPS_PASSWORD${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    ssh-copy-id -i "${SSH_KEY_FILE}.pub" -p "$VPS_SSH_PORT" "$VPS_USER@$VPS_HOST" || {
        echo ""
        echo -e "${YELLOW}⚠️  Intentando método manual...${NC}"
        cat "${SSH_KEY_FILE}.pub" | ssh -p "$VPS_SSH_PORT" "$VPS_USER@$VPS_HOST" \
            "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    }

    echo -e "${GREEN}✅ Clave copiada${NC}"

    echo ""
    echo -e "${GREEN}3️⃣  Verificando conexión...${NC}"
    ssh -i "$SSH_KEY_FILE" -p "$VPS_SSH_PORT" -o BatchMode=yes "$VPS_USER@$VPS_HOST" "echo '✅ SSH sin password OK'" || {
        echo -e "${RED}❌ Error: Conexión sin password falló${NC}"
        exit 1
    }

    echo ""
    echo -e "${GREEN}4️⃣  Configurando ~/.ssh/config...${NC}"
    SSH_CONFIG="$HOME/.ssh/config"
    touch "$SSH_CONFIG"
    chmod 600 "$SSH_CONFIG"

    # Remover entradas antiguas
    sed -i.bak '/^# SAAS Platform VPS/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true
    sed -i.bak '/^Host saas-vps/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true

    # Agregar nueva entrada
    cat >> "$SSH_CONFIG" << SSHEOF

# SAAS Platform VPS
Host saas-vps
    HostName $VPS_HOST
    User $VPS_USER
    Port $VPS_SSH_PORT
    IdentityFile $SSH_KEY_FILE
    ServerAliveInterval 60
    ServerAliveCountMax 3
    StrictHostKeyChecking no
SSHEOF

    echo -e "${GREEN}✅ Configuración SSH completa${NC}"
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}🎉 Ya puedes desplegar con: ./remote-deploy.sh${NC}"
    echo -e "${GREEN}   O conectarte con: ssh saas-vps${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    exit 0
}

# ============================================
# PARSEAR ARGUMENTOS
# ============================================

if [ "$1" = "--setup-ssh" ] || [ "$1" = "-s" ]; then
    setup_ssh
fi

# ============================================
# BANNER
# ============================================

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🚀 REMOTE DEPLOYMENT - LINUX/MAC        ║
║   SSH Keys - Sin Password                 ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# ============================================
# VALIDAR CONFIG
# ============================================

if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}❌ No se encontró: $CONFIG_FILE${NC}"
    echo ""
    echo -e "${YELLOW}Creando configuración por defecto...${NC}"

    cat > "$CONFIG_FILE" << 'CONFIGEOF'
# Ver archivo completo en el artifact anterior
VPS_HOST=72.62.174.193
VPS_USER=root
VPS_PATH=/opt/saas-platform
VPS_SSH_PORT=22

DEPLOY_MYSQL=true
DEPLOY_CONFIG_SERVER=true
SERVICE_PORT_CONFIG_SERVER=8888
DEPLOY_DISCOVERY_SERVICE=true
SERVICE_PORT_DISCOVERY_SERVICE=8761
DEPLOY_AUTH_SERVICE=true
SERVICE_PORT_AUTH_SERVICE=8082
DEPLOY_SYSTEM_SERVICE=true
SERVICE_PORT_SYSTEM_SERVICE=8083
DEPLOY_GATEWAY_SERVICE=true
SERVICE_PORT_GATEWAY_SERVICE=8080

FORCE_REBUILD=false
SKIP_BACKUP=false
AUTO_PULL=true
GIT_BRANCH=main
SHOW_LOGS=true
HEALTH_CHECK_TIMEOUT=120
HEALTH_CHECK_INTERVAL=5
CONFIGEOF

    echo -e "${GREEN}✅ Creado: $CONFIG_FILE${NC}"
    echo ""
    read -p "¿Editar ahora? (s/n): " EDIT
    [ "$EDIT" = "s" ] && ${EDITOR:-nano} "$CONFIG_FILE"
fi

source "$CONFIG_FILE"

# Validar variables críticas
[ -z "$VPS_HOST" ] && echo -e "${RED}❌ VPS_HOST no definido${NC}" && exit 1
[ -z "$VPS_USER" ] && echo -e "${RED}❌ VPS_USER no definido${NC}" && exit 1
[ -z "$VPS_PATH" ] && echo -e "${RED}❌ VPS_PATH no definido${NC}" && exit 1

echo -e "${GREEN}✅ Configuración cargada${NC}"
echo "   Host: $VPS_HOST"
echo "   Path: $VPS_PATH"
echo ""

# ============================================
# VERIFICAR SSH KEY
# ============================================

if [ ! -f "$SSH_KEY_FILE" ]; then
    echo -e "${YELLOW}⚠️  No se encontró clave SSH${NC}"
    echo ""
    echo "Opciones:"
    echo "  1. Configurar SSH keys (recomendado)"
    echo "  2. Usar password manualmente (cada vez)"
    echo ""
    read -p "Selecciona (1/2): " OPTION

    if [ "$OPTION" = "1" ]; then
        setup_ssh
    else
        USE_SSH_KEY=false
        SSH_CMD="ssh -p $VPS_SSH_PORT $VPS_USER@$VPS_HOST"
        SCP_CMD="scp -P $VPS_SSH_PORT"
        echo -e "${YELLOW}⚠️  Se pedirá password en cada conexión${NC}"
    fi
else
    USE_SSH_KEY=true
    SSH_CMD="ssh -i $SSH_KEY_FILE -p $VPS_SSH_PORT $VPS_USER@$VPS_HOST"
    SCP_CMD="scp -i $SSH_KEY_FILE -P $VPS_SSH_PORT"
    echo -e "${GREEN}✅ Usando clave SSH: $SSH_KEY_FILE${NC}"
fi

echo ""

# ============================================
# DETECTAR SERVICIOS
# ============================================

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

[ $SERVICE_COUNT -eq 0 ] && echo -e "${RED}❌ No hay servicios habilitados${NC}" && exit 1

echo ""
echo -e "${GREEN}📊 Total: $SERVICE_COUNT servicios${NC}"
echo ""

# Confirmar
read -p "¿Continuar? (s/n): " CONFIRM
[ "$CONFIRM" != "s" ] && echo "❌ Cancelado" && exit 0

# ============================================
# VERIFICAR CONEXIÓN
# ============================================

echo ""
echo -e "${GREEN}1️⃣  Verificando conexión...${NC}"

eval "$SSH_CMD 'echo OK'" > /dev/null || {
    echo -e "${RED}❌ No se pudo conectar al VPS${NC}"
    echo ""
    echo -e "${YELLOW}💡 Opciones:${NC}"
    echo "  - Configura SSH keys: ./remote-deploy.sh --setup-ssh"
    echo "  - Verifica que el VPS esté accesible"
    exit 1
}

echo -e "${GREEN}✅ Conexión OK${NC}"

# ============================================
# SUBIR ARCHIVOS
# ============================================

echo ""
echo -e "${GREEN}2️⃣  Subiendo archivos...${NC}"

echo "   📤 deploy-config.env..."
eval "$SCP_CMD '$CONFIG_FILE' $VPS_USER@$VPS_HOST:$VPS_PATH/" || exit 1

echo "   📤 deploy.sh..."
eval "$SCP_CMD deploy.sh $VPS_USER@$VPS_HOST:$VPS_PATH/" || exit 1

echo -e "${GREEN}✅ Archivos subidos${NC}"

# ============================================
# EJECUTAR DEPLOYMENT
# ============================================

echo ""
echo -e "${GREEN}3️⃣  Configurando permisos...${NC}"
eval "$SSH_CMD 'cd $VPS_PATH && chmod +x deploy.sh'"

echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   🚀 EJECUTANDO DEPLOYMENT EN VPS         ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════╝${NC}"
echo ""

eval "$SSH_CMD 'cd $VPS_PATH && ./deploy.sh'"
DEPLOY_EXIT=$?

# ============================================
# RESULTADO
# ============================================

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $DEPLOY_EXIT -eq 0 ]; then
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ✅ DEPLOYMENT COMPLETADO                ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
else
    echo -e "${YELLOW}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   ⚠️  DEPLOYMENT CON ADVERTENCIAS         ║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════╝${NC}"
fi

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}📝 Comandos útiles:${NC}"
echo ""
echo "🔍 Ver logs en vivo:"
echo "   ssh saas-vps 'cd $VPS_PATH && docker compose logs -f'"
echo ""
echo "📊 Ver estado:"
echo "   ssh saas-vps 'cd $VPS_PATH && docker compose ps'"
echo ""
echo "🔄 Reiniciar servicio:"
echo "   ssh saas-vps 'cd $VPS_PATH && docker compose restart <servicio>'"
echo ""

exit $DEPLOY_EXIT