#!/bin/bash

# ============================================
# SCRIPT DE REINICIO COMPLETO Y LIMPIEZA
# Usa cuando quieras empezar desde cero
# ============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║   🔄 REINICIO COMPLETO DEL SISTEMA        ║
║   ⚠️  ELIMINA TODO Y RECONSTRUYE          ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

echo -e "${YELLOW}⚠️  ADVERTENCIA: Esto eliminará TODOS los contenedores, volúmenes e imágenes${NC}"
echo -e "${YELLOW}   Solo la base de datos se preservará si no usas -v${NC}"
echo ""
read -p "¿Continuar? (escribe 'SI' en mayúsculas): " CONFIRM

if [ "$CONFIRM" != "SI" ]; then
    echo "❌ Operación cancelada"
    exit 0
fi

echo ""
echo -e "${GREEN}1️⃣  Deteniendo y eliminando contenedores...${NC}"
docker compose down 2>/dev/null || true

echo ""
echo -e "${GREEN}2️⃣  Eliminando contenedores antiguos...${NC}"
docker compose rm -f 2>/dev/null || true

echo ""
echo -e "${GREEN}3️⃣  Eliminando todas las imágenes SAAS...${NC}"
docker images | grep "saas-" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
echo "✅ Imágenes SAAS eliminadas"

echo ""
echo -e "${GREEN}4️⃣  Limpiando imágenes huérfanas...${NC}"
docker image prune -af --filter "label!=keep" 2>/dev/null || true

echo ""
echo -e "${GREEN}5️⃣  Verificando archivo .env...${NC}"
if [ ! -f .env ]; then
    echo -e "${RED}❌ ERROR: Archivo .env no encontrado${NC}"
    exit 1
fi

echo "✅ Archivo .env encontrado:"
cat .env
echo ""

echo -e "${GREEN}6️⃣  Construyendo servicios desde cero (sin caché)...${NC}"
echo "   ⏱️  Esto tomará 10-15 minutos..."
docker compose build --no-cache --pull

echo ""
echo -e "${GREEN}7️⃣  Iniciando servicios...${NC}"
docker compose up -d

echo ""
echo -e "${GREEN}8️⃣  Esperando que los servicios estén listos...${NC}"

wait_for() {
    local name=$1
    local port=$2
    echo -n "   Esperando $name (puerto $port)... "
    for i in {1..60}; do
        if curl -sf http://localhost:$port/actuator/health >/dev/null 2>&1; then
            echo -e "${GREEN}✅${NC}"
            return 0
        fi
        sleep 3
    done
    echo -e "${YELLOW}⚠️  Timeout${NC}"
}

sleep 20
wait_for "Config Server" 8888
wait_for "Discovery" 8761
wait_for "Auth Service" 8082
wait_for "System Service" 8083
wait_for "Gateway" 8080

echo ""
echo -e "${GREEN}9️⃣  Estado de los servicios:${NC}"
docker compose ps

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ REINICIO COMPLETADO                  ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
echo ""
echo "📝 Comandos útiles:"
echo "   Ver logs:        docker compose logs -f [servicio]"
echo "   Ver todos logs:  docker compose logs -f"
echo "   Detener todo:    docker compose down"
echo ""
echo "🌐 URLs de servicios:"
IP=$(hostname -I | awk '{print $1}')
echo "   Gateway:  http://${IP}:8080"
echo "   Eureka:   http://${IP}:8761"
echo "   Auth:     http://${IP}:8082"
echo "   System:   http://${IP}:8083"
echo ""