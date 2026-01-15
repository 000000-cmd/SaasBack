#!/bin/bash

# ============================================
# SCRIPT DE SETUP INICIAL DEL SERVIDOR
# Ejecutar UNA SOLA VEZ en servidor nuevo
# ============================================

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🔧 SAAS PLATFORM - SERVER SETUP         ║
║   Ubuntu 22.04+ con Docker                ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Verificar que se ejecuta como root
if [ "$EUID" -ne 0 ]; then
    echo "❌ Este script debe ejecutarse como root (usa sudo)"
    exit 1
fi

echo -e "${GREEN}1️⃣  Actualizando sistema...${NC}"
apt-get update
apt-get upgrade -y

echo -e "${GREEN}2️⃣  Instalando dependencias...${NC}"
apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    htop \
    vim \
    ufw \
    fail2ban

echo -e "${GREEN}3️⃣  Instalando Docker...${NC}"
# Remover versiones antiguas
apt-get remove -y docker docker-engine docker.io containerd runc || true

# Agregar repositorio de Docker
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Iniciar Docker
systemctl start docker
systemctl enable docker

echo -e "${GREEN}4️⃣  Configurando firewall (UFW)...${NC}"
ufw --force enable
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment "SSH"
ufw allow 80/tcp comment "HTTP"
ufw allow 443/tcp comment "HTTPS"
ufw allow 8080/tcp comment "Gateway"
echo "y" | ufw enable

echo -e "${GREEN}5️⃣  Creando directorios...${NC}"
mkdir -p /opt/saas-platform
mkdir -p /opt/saas-backups
mkdir -p /var/log

echo -e "${GREEN}6️⃣  Configurando Git...${NC}"
cd /opt/saas-platform

# Configurar Git si no está configurado
if [ ! -d .git ]; then
    echo -e "${YELLOW}Ingresa la URL del repositorio Git:${NC}"
    read -p "URL: " REPO_URL

    git clone "$REPO_URL" .

    echo -e "${YELLOW}¿Necesitas configurar credenciales de Git? (s/n):${NC}"
    read -p "> " SETUP_GIT

    if [ "$SETUP_GIT" = "s" ]; then
        read -p "Usuario Git: " GIT_USER
        read -p "Email Git: " GIT_EMAIL
        git config --global user.name "$GIT_USER"
        git config --global user.email "$GIT_EMAIL"

        echo "Para autenticación, usa Personal Access Token en lugar de password"
        echo "GitHub: Settings > Developer settings > Personal access tokens"
    fi
else
    echo "Repositorio ya clonado"
fi

echo -e "${GREEN}7️⃣  Configurando .env...${NC}"
if [ ! -f /opt/saas-platform/.env ]; then
    cat > /opt/saas-platform/.env << 'ENVEOF'
# ===========================================
# BASE DE DATOS MYSQL
# ===========================================
MYSQL_ROOT_PASSWORD=CAMBIAR_ESTE_PASSWORD_AHORA
MYSQL_DATABASE=saas_db

# ===========================================
# TIMEZONE
# ===========================================
TZ=America/Bogota
ENVEOF

    echo -e "${YELLOW}⚠️  IMPORTANTE: Edita /opt/saas-platform/.env y cambia el password de MySQL${NC}"
    echo -e "${YELLOW}nano /opt/saas-platform/.env${NC}"
else
    echo ".env ya existe"
fi

echo -e "${GREEN}8️⃣  Configurando permisos...${NC}"
chmod +x /opt/saas-platform/deploy.sh
chown -R $SUDO_USER:$SUDO_USER /opt/saas-platform

echo -e "${GREEN}9️⃣  Configurando cron para deployment automático (opcional)...${NC}"
echo "¿Quieres configurar deployment automático cada noche? (s/n)"
read -p "> " SETUP_CRON

if [ "$SETUP_CRON" = "s" ]; then
    # Backup a las 2 AM
    (crontab -l 2>/dev/null; echo "0 2 * * * /opt/saas-platform/backup.sh >> /var/log/saas-backup.log 2>&1") | crontab -
    echo "✅ Cron configurado"
fi

echo -e "${GREEN}🔟 Configurando log rotation...${NC}"
cat > /etc/logrotate.d/saas-platform << 'LOGEOF'
/var/log/saas-*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
LOGEOF

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ SERVIDOR CONFIGURADO EXITOSAMENTE    ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo ""
echo "📋 PRÓXIMOS PASOS:"
echo ""
echo "1️⃣  Edita las variables de entorno:"
echo "   nano /opt/saas-platform/.env"
echo ""
echo "2️⃣  Ejecuta el primer deployment:"
echo "   cd /opt/saas-platform"
echo "   ./deploy.sh"
echo ""
echo "3️⃣  Verifica que todo está corriendo:"
echo "   docker-compose ps"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "4️⃣  Configura tu dominio (opcional):"
echo "   - Apunta tu dominio a: $(hostname -I | awk '{print $1}')"
echo "   - Instala Nginx reverse proxy"
echo "   - Configura SSL con Let's Encrypt"
echo ""
echo "📝 Comandos útiles:"
echo "   Ver logs: docker-compose logs -f"
echo "   Reiniciar: docker-compose restart"
echo "   Detener: docker-compose down"
echo "   Deploy: ./deploy.sh"
echo ""