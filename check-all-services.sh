#!/bin/bash

# ============================================
# VERIFICACIÓN COMPLETA DE SERVICIOS
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_service() {
    local service_name=$1
    local port=$2
    local endpoint=${3:-/actuator/health}

    echo -n "Verificando $service_name (puerto $port)... "

    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port$endpoint 2>/dev/null)

    if [ "$response" == "200" ]; then
        echo -e "${GREEN}✅ OK${NC}"
        return 0
    else
        echo -e "${RED}❌ FAIL (HTTP $response)${NC}"
        return 1
    fi
}

echo "============================================"
echo "🔍 VERIFICACIÓN DE SERVICIOS"
echo "============================================"
echo ""

# Verificar contenedores
echo "📦 Contenedores Docker:"
docker-compose ps
echo ""

# Verificar servicios
echo "🌐 Estado de los servicios:"
echo ""

check_service "Config Server" 8888
check_service "Discovery (Eureka)" 8761
check_service "Auth Service" 8082
check_service "System Service" 8083
check_service "Gateway" 8080

echo ""
echo "============================================"
echo "📊 SERVICIOS REGISTRADOS EN EUREKA"
echo "============================================"
echo ""

curl -s http://localhost:8761/eureka/apps | grep -o '<name>[^<]*</name>' | sed 's/<\/*name>//g' | sort -u

echo ""
echo ""
echo "============================================"
echo "📝 LOGS RECIENTES"
echo "============================================"
echo ""
echo "Auth Service:"
docker-compose logs --tail=5 auth-service
echo ""
echo "System Service:"
docker-compose logs --tail=5 system-service
echo ""
echo "Gateway:"
docker-compose logs --tail=5 gateway-service

echo ""
echo "============================================"
echo "✅ Verificación completada"
echo "============================================"