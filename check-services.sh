#!/bin/bash

# ============================================
# VERIFICACION DE SERVICIOS - SAAS PLATFORM
# ============================================
# Verifica el estado de todos los servicios
# y muestra informacion util de diagnostico
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
BOLD='\033[1m'

# ============================================
# FUNCIONES
# ============================================
print_header() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN} $1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

check_service() {
    local service_name=$1
    local port=$2
    local endpoint=${3:-/actuator/health}

    printf "  %-25s " "$service_name (puerto $port)..."

    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}${endpoint}" 2>/dev/null || echo "000")

    if [ "$response" == "200" ]; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}FAIL (HTTP $response)${NC}"
        return 1
    fi
}

check_container() {
    local container_name=$1

    printf "  %-25s " "$container_name..."

    local status
    status=$(docker inspect --format='{{.State.Status}}' "$container_name" 2>/dev/null || echo "not_found")

    local health
    health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")

    if [ "$status" == "running" ]; then
        if [ "$health" == "healthy" ]; then
            echo -e "${GREEN}Running (healthy)${NC}"
        elif [ "$health" == "none" ]; then
            echo -e "${GREEN}Running${NC}"
        else
            echo -e "${YELLOW}Running ($health)${NC}"
        fi
        return 0
    elif [ "$status" == "not_found" ]; then
        echo -e "${RED}No existe${NC}"
        return 1
    else
        echo -e "${RED}$status${NC}"
        return 1
    fi
}

# ============================================
# BANNER
# ============================================
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       VERIFICACION DE SERVICIOS - SAAS PLATFORM              ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"

# ============================================
# 1. ESTADO DE CONTENEDORES
# ============================================
print_header "ESTADO DE CONTENEDORES"

check_container "saas-mysql"
check_container "saas-config-server"
check_container "saas-discovery"
check_container "saas-auth"
check_container "saas-system"
check_container "saas-gateway"

# ============================================
# 2. HEALTH CHECKS HTTP
# ============================================
print_header "HEALTH CHECKS HTTP"

check_service "Config Server" 8888
check_service "Discovery (Eureka)" 8761
check_service "Auth Service" 8082
check_service "System Service" 8083
check_service "Gateway" 8080

# ============================================
# 3. SERVICIOS EN EUREKA
# ============================================
print_header "SERVICIOS REGISTRADOS EN EUREKA"

eureka_apps=$(curl -s http://localhost:8761/eureka/apps 2>/dev/null || echo "")

if echo "$eureka_apps" | grep -q "<application>"; then
    echo "$eureka_apps" | grep -oP '<name>\K[^<]+' 2>/dev/null | sort -u | while read -r service; do
        echo -e "  ${GREEN}✓${NC} $service"
    done
else
    echo -e "  ${YELLOW}No se pudieron obtener servicios de Eureka${NC}"
fi

# ============================================
# 4. USO DE RECURSOS
# ============================================
print_header "USO DE RECURSOS"

docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" 2>/dev/null | head -10 || echo "  No se pudo obtener estadisticas"

# ============================================
# 5. PUERTOS EN USO
# ============================================
print_header "PUERTOS EN USO"

echo "  Puerto   Estado"
echo "  ------   ------"

for port in 3306 8888 8761 8082 8083 8080; do
    if netstat -tuln 2>/dev/null | grep -q ":$port " || ss -tuln 2>/dev/null | grep -q ":$port "; then
        printf "  %-8s ${GREEN}En uso${NC}\n" "$port"
    else
        printf "  %-8s ${RED}Libre${NC}\n" "$port"
    fi
done

# ============================================
# 6. LOGS RECIENTES (errores)
# ============================================
print_header "ERRORES RECIENTES EN LOGS"

for container in saas-auth saas-system saas-gateway; do
    errors=$(docker logs --tail 50 "$container" 2>&1 | grep -i "error\|exception" | tail -3 || true)
    if [ -n "$errors" ]; then
        echo -e "  ${YELLOW}$container:${NC}"
        echo "$errors" | head -3 | sed 's/^/    /'
    fi
done

echo ""

# ============================================
# 7. RESUMEN
# ============================================
print_header "RESUMEN"

# Contar servicios healthy
healthy_count=$(docker ps --filter "health=healthy" --format "{{.Names}}" | grep -c "saas-" || echo "0")
running_count=$(docker ps --filter "name=saas-" --format "{{.Names}}" | wc -l)
total_count=6

echo -e "  Contenedores corriendo:  ${running_count}/${total_count}"
echo -e "  Contenedores healthy:    ${healthy_count}/${total_count}"

if [ "$healthy_count" -ge 5 ]; then
    echo ""
    echo -e "  ${GREEN}Estado general: OPERATIVO${NC}"
else
    echo ""
    echo -e "  ${YELLOW}Estado general: PARCIALMENTE OPERATIVO${NC}"
    echo -e "  ${YELLOW}Ejecuta: docker compose logs [servicio] para mas detalles${NC}"
fi

echo ""
echo -e "${CYAN}============================================${NC}"
echo ""

# ============================================
# COMANDOS UTILES
# ============================================
echo "Comandos utiles:"
echo "  docker compose logs -f              # Ver todos los logs"
echo "  docker compose logs -f saas-gateway # Ver logs de un servicio"
echo "  docker compose restart              # Reiniciar todos"
echo "  docker compose restart saas-auth    # Reiniciar un servicio"
echo ""