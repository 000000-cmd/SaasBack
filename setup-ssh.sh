#!/bin/bash

# ============================================
# CONFIGURACIÓN SEGURA DE SSH
# Crear clave SSH y configurar acceso sin password
# ============================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🔐 CONFIGURACIÓN SEGURA DE SSH          ║
║   Acceso sin password al VPS              ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Configuración
VPS_HOST="72.62.174.193"
VPS_USER="root"
SSH_KEY_FILE="$HOME/.ssh/saas_vps_key"

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📋 Configuración:${NC}"
echo "   VPS Host: $VPS_HOST"
echo "   Usuario: $VPS_USER"
echo "   Clave SSH: $SSH_KEY_FILE"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Verificar si ya existe la clave
if [ -f "$SSH_KEY_FILE" ]; then
    echo -e "${YELLOW}⚠️  Ya existe una clave SSH en:${NC}"
    echo "   $SSH_KEY_FILE"
    echo ""
    echo "¿Quieres crear una nueva? Esto sobrescribirá la anterior (s/n):"
    read -p "> " OVERWRITE

    if [ "$OVERWRITE" != "s" ]; then
        echo ""
        echo -e "${GREEN}✅ Usando clave existente${NC}"
        USE_EXISTING=true
    else
        USE_EXISTING=false
    fi
else
    USE_EXISTING=false
fi

# Crear clave SSH si es necesario
if [ "$USE_EXISTING" != "true" ]; then
    echo ""
    echo -e "${GREEN}1️⃣  Generando clave SSH...${NC}"

    # Crear directorio .ssh si no existe
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh

    # Generar clave
    ssh-keygen -t ed25519 -f "$SSH_KEY_FILE" -N "" -C "saas-deploy-key-$(date +%Y%m%d)" || {
        echo -e "${RED}❌ Error al generar clave SSH${NC}"
        exit 1
    }

    chmod 600 "$SSH_KEY_FILE"
    chmod 644 "${SSH_KEY_FILE}.pub"

    echo -e "${GREEN}✅ Clave SSH generada exitosamente${NC}"
    echo "   Privada: $SSH_KEY_FILE"
    echo "   Pública: ${SSH_KEY_FILE}.pub"
fi

# Copiar clave al servidor
echo ""
echo -e "${GREEN}2️⃣  Copiando clave pública al servidor VPS...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}⚠️  Se te pedirá la CONTRASEÑA DEL VPS${NC}"
echo -e "${YELLOW}   Esta es la ÚLTIMA VEZ que la necesitarás${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Intentar ssh-copy-id primero (método recomendado)
if command -v ssh-copy-id &> /dev/null; then
    echo "📤 Usando ssh-copy-id..."
    echo ""

    ssh-copy-id -i "${SSH_KEY_FILE}.pub" "$VPS_USER@$VPS_HOST" || {
        echo ""
        echo -e "${YELLOW}⚠️  ssh-copy-id falló, intentando método manual...${NC}"
        echo ""

        # Método manual
        cat "${SSH_KEY_FILE}.pub" | ssh "$VPS_USER@$VPS_HOST" "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys" || {
            echo -e "${RED}❌ Error al copiar clave al servidor${NC}"
            echo ""
            echo -e "${YELLOW}💡 Verifica:${NC}"
            echo "   - La contraseña del VPS es correcta"
            echo "   - El servidor está accesible"
            echo "   - SSH está habilitado en el servidor"
            exit 1
        }
    }
else
    # Si no existe ssh-copy-id, usar método manual directamente
    echo "📤 Copiando clave manualmente..."
    echo ""

    cat "${SSH_KEY_FILE}.pub" | ssh "$VPS_USER@$VPS_HOST" "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys" || {
        echo -e "${RED}❌ Error al copiar clave al servidor${NC}"
        exit 1
    }
fi

echo ""
echo -e "${GREEN}✅ Clave copiada exitosamente${NC}"

# Verificar conexión sin password
echo ""
echo -e "${GREEN}3️⃣  Verificando conexión SSH sin password...${NC}"

ssh -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no -o BatchMode=yes "$VPS_USER@$VPS_HOST" "echo '✅ Conexión SSH sin password funcionando correctamente'" || {
    echo -e "${RED}❌ Error: La conexión sin password no funciona${NC}"
    echo ""
    echo -e "${YELLOW}💡 Posibles causas:${NC}"
    echo "   1. Permisos incorrectos en el servidor"
    echo "   2. SSH configurado para no permitir autenticación por clave"
    echo ""
    echo "Intentando diagnosticar..."
    ssh -i "$SSH_KEY_FILE" "$VPS_USER@$VPS_HOST" "ls -la ~/.ssh/"
    exit 1
}

echo -e "${GREEN}✅ Conexión verificada${NC}"

# Configurar SSH config para acceso fácil
echo ""
echo -e "${GREEN}4️⃣  Configurando ~/.ssh/config...${NC}"

SSH_CONFIG="$HOME/.ssh/config"

# Crear archivo config si no existe
touch "$SSH_CONFIG"
chmod 600 "$SSH_CONFIG"

# Verificar si ya existe la entrada
if grep -q "Host saas-vps" "$SSH_CONFIG" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  La entrada 'saas-vps' ya existe en $SSH_CONFIG${NC}"
    echo "¿Quieres actualizarla? (s/n):"
    read -p "> " UPDATE_CONFIG

    if [ "$UPDATE_CONFIG" = "s" ]; then
        # Remover entrada antigua
        sed -i.bak '/^# SAAS Platform VPS/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true
        sed -i.bak '/^Host saas-vps/,/^$/d' "$SSH_CONFIG" 2>/dev/null || true

        # Agregar nueva entrada
        cat >> "$SSH_CONFIG" << SSHEOF

# SAAS Platform VPS
Host saas-vps
    HostName $VPS_HOST
    User $VPS_USER
    IdentityFile $SSH_KEY_FILE
    ServerAliveInterval 60
    ServerAliveCountMax 3
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
SSHEOF
        echo -e "${GREEN}✅ Configuración actualizada${NC}"
    else
        echo -e "${YELLOW}⏭️  Saltando actualización de config${NC}"
    fi
else
    # Agregar nueva entrada
    cat >> "$SSH_CONFIG" << SSHEOF

# SAAS Platform VPS
Host saas-vps
    HostName $VPS_HOST
    User $VPS_USER
    IdentityFile $SSH_KEY_FILE
    ServerAliveInterval 60
    ServerAliveCountMax 3
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
SSHEOF
    echo -e "${GREEN}✅ Configuración SSH agregada${NC}"
fi

# Test final
echo ""
echo -e "${GREEN}5️⃣  Test final de conexión...${NC}"

ssh saas-vps "echo '✅ Alias funcionando correctamente'" || {
    echo -e "${YELLOW}⚠️  Alias no funciona, pero puedes usar la clave directamente${NC}"
}

# Resumen final
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ SSH CONFIGURADO EXITOSAMENTE         ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}📝 Formas de conectarte ahora:${NC}"
echo ""
echo "   1️⃣  Usando alias (recomendado):"
echo "      ${CYAN}ssh saas-vps${NC}"
echo ""
echo "   2️⃣  Usando clave directamente:"
echo "      ${CYAN}ssh -i $SSH_KEY_FILE $VPS_USER@$VPS_HOST${NC}"
echo ""
echo "   3️⃣  IP directa:"
echo "      ${CYAN}ssh $VPS_USER@$VPS_HOST${NC}"
echo ""
echo -e "${GREEN}🚀 Para desplegar automáticamente:${NC}"
echo "   ${CYAN}./remote-deploy.sh${NC}"
echo ""
echo -e "${GREEN}🔐 Tu clave SSH está en:${NC}"
echo "   ${CYAN}$SSH_KEY_FILE${NC}"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANTE: Guarda esta clave de forma segura${NC}"
echo -e "${YELLOW}   Si la pierdes, tendrás que reconfigurar SSH${NC}"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}✅ Ya no necesitarás password para conectarte${NC}"
echo -e "${GREEN}✅ El despliegue automático está listo${NC}"
echo ""