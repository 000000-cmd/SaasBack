#!/bin/bash

# ============================================
# SETUP INICIAL DEL SERVIDOR - SAAS PLATFORM
# ============================================
# Ejecutar UNA SOLA VEZ en servidor nuevo (Ubuntu 22.04+)
# Este script instala todas las dependencias necesarias
# ============================================

set -euo pipefail

# ============================================
# COLORES
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================
# FUNCIONES
# ============================================
log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ERROR:${NC} $1"
    exit 1
}

warning() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] ADVERTENCIA:${NC} $1"
}

# ============================================
# VERIFICAR ROOT
# ============================================
if [ "$EUID" -ne 0 ]; then
    error "Este script debe ejecutarse como root (usa sudo)"
fi

# ============================================
# BANNER
# ============================================
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       SETUP INICIAL - SAAS PLATFORM                          ║${NC}"
echo -e "${CYAN}║       Ubuntu 22.04+ con Docker                               ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================
# 1. ACTUALIZAR SISTEMA
# ============================================
log "Actualizando sistema..."
apt-get update -qq
apt-get upgrade -y -qq

# ============================================
# 2. INSTALAR DEPENDENCIAS BASICAS
# ============================================
log "Instalando dependencias basicas..."
apt-get install -y -qq \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    htop \
    vim \
    nano \
    ufw \
    fail2ban \
    net-tools \
    unzip \
    jq

# ============================================
# 3. INSTALAR DOCKER
# ============================================
log "Instalando Docker..."

# Remover versiones antiguas
apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

# Agregar repositorio de Docker
install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg 2>/dev/null
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker
apt-get update -qq
apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Iniciar y habilitar Docker
systemctl start docker
systemctl enable docker

log "Docker instalado: $(docker --version)"

# ============================================
# 4. CONFIGURAR FIREWALL
# ============================================
log "Configurando firewall (UFW)..."

ufw --force reset
ufw default deny incoming
ufw default allow outgoing

# Puertos necesarios
ufw allow 22/tcp comment "SSH"
ufw allow 80/tcp comment "HTTP"
ufw allow 443/tcp comment "HTTPS"
ufw allow 8080/tcp comment "Gateway"
ufw allow 8761/tcp comment "Eureka Dashboard"

# Habilitar firewall
echo "y" | ufw enable

log "Firewall configurado"

# ============================================
# 5. CREAR DIRECTORIOS
# ============================================
log "Creando directorios..."

mkdir -p /opt/saas-platform
mkdir -p /opt/saas-backups
mkdir -p /var/log

# ============================================
# 6. CONFIGURAR GIT Y CLONAR REPOSITORIO
# ============================================
log "Configurando Git..."

cd /opt/saas-platform

if [ ! -d .git ]; then
    echo ""
    echo -e "${YELLOW}Ingresa la URL del repositorio Git:${NC}"
    echo "Ejemplo: https://github.com/tu-usuario/saas-platform.git"
    read -rp "URL: " REPO_URL

    if [ -n "$REPO_URL" ]; then
        git clone "$REPO_URL" . || error "Error clonando repositorio"
        log "Repositorio clonado"
    else
        warning "No se especifico repositorio, continuar manualmente"
    fi
else
    log "Repositorio ya existe"
fi

# ============================================
# 7. CREAR ARCHIVO .ENV
# ============================================
log "Configurando variables de entorno..."

if [ ! -f /opt/saas-platform/.env ]; then
    cat > /opt/saas-platform/.env << 'ENVFILE'
# ===========================================
# VARIABLES DE ENTORNO - SAAS PLATFORM
# ===========================================
# IMPORTANTE: Cambiar estos valores en produccion!
# ===========================================

# Base de datos
MYSQL_ROOT_PASSWORD=CAMBIAR_PASSWORD_SEGURO
MYSQL_DATABASE=saas_db

# Timezone
TZ=America/Bogota
ENVFILE

    warning "Archivo .env creado con valores por defecto"
    warning "IMPORTANTE: Edita /opt/saas-platform/.env y cambia el password"
else
    log "Archivo .env ya existe"
fi

# ============================================
# 8. CONFIGURAR PERMISOS
# ============================================
log "Configurando permisos..."

chmod +x /opt/saas-platform/*.sh 2>/dev/null || true
chown -R root:root /opt/saas-platform

# ============================================
# 9. CONFIGURAR CRON PARA BACKUPS
# ============================================
log "Configurando backup automatico..."

# Agregar cron para backup diario a las 2 AM
(crontab -l 2>/dev/null | grep -v "saas-platform/backup.sh"; echo "0 2 * * * /opt/saas-platform/backup.sh >> /var/log/saas-backup.log 2>&1") | crontab -

log "Backup automatico configurado (2:00 AM diario)"

# ============================================
# 10. CONFIGURAR LOG ROTATION
# ============================================
log "Configurando rotacion de logs..."

cat > /etc/logrotate.d/saas-platform << 'LOGROTATE'
/var/log/saas-*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
LOGROTATE

# ============================================
# 11. OPTIMIZACIONES DEL SISTEMA
# ============================================
log "Aplicando optimizaciones del sistema..."

# Aumentar limites de archivos abiertos
cat >> /etc/security/limits.conf << 'LIMITS'
* soft nofile 65535
* hard nofile 65535
root soft nofile 65535
root hard nofile 65535
LIMITS

# Optimizaciones de red para Docker
cat >> /etc/sysctl.conf << 'SYSCTL'
# Optimizaciones para Docker
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.ip_forward = 1
vm.max_map_count = 262144
SYSCTL

sysctl -p 2>/dev/null || true

# ============================================
# RESUMEN FINAL
# ============================================
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║       SERVIDOR CONFIGURADO EXITOSAMENTE                      ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

IP_ADDR=$(hostname -I | awk '{print $1}')

echo "PROXIMOS PASOS:"
echo ""
echo "1. Editar variables de entorno:"
echo "   nano /opt/saas-platform/.env"
echo ""
echo "2. Ejecutar el primer deployment:"
echo "   cd /opt/saas-platform"
echo "   ./deploy.sh"
echo ""
echo "3. Verificar que todo funciona:"
echo "   ./check-services.sh"
echo ""
echo "4. Acceder a los servicios:"
echo "   Gateway:  http://${IP_ADDR}:8080"
echo "   Eureka:   http://${IP_ADDR}:8761"
echo ""
echo "5. (Opcional) Configurar dominio y SSL:"
echo "   - Apuntar tu dominio a: ${IP_ADDR}"
echo "   - Instalar Nginx como reverse proxy"
echo "   - Configurar SSL con Let's Encrypt"
echo ""

warning "IMPORTANTE: Cambia el password de MySQL en /opt/saas-platform/.env"
echo ""