#!/bin/bash

# ============================================
# SCRIPT DE REINICIO COMPLETO
# ============================================

echo "🧹 Limpiando contenedores y volúmenes antiguos..."
docker-compose down -v

echo ""
echo "🗑️  Eliminando imágenes antiguas..."
docker-compose rm -f
docker images | grep "saas" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true

echo ""
echo "🔧 Verificando archivo .env..."
if [ ! -f .env ]; then
    echo "❌ ERROR: Archivo .env no encontrado"
    exit 1
fi

echo "✅ Archivo .env encontrado"
cat .env

echo ""
echo "🏗️  Construyendo servicios desde cero..."
docker-compose build --no-cache

echo ""
echo "🚀 Iniciando servicios..."
docker-compose up -d

echo ""
echo "⏳ Esperando que los servicios estén listos..."
echo "   - Config Server (60s)"
sleep 60

echo "   - Discovery Service (30s)"
sleep 30

echo "   - MySQL (verificando...)"
docker-compose logs mysql | tail -20

echo ""
echo "📊 Estado de los servicios:"
docker-compose ps

echo ""
echo "📝 Ver logs en tiempo real:"
echo "   docker-compose logs -f"
echo ""
echo "📝 Ver logs de un servicio específico:"
echo "   docker-compose logs -f auth-service"
echo "   docker-compose logs -f system-service"
echo ""
echo "🌐 URLs importantes:"
echo "   - Eureka: http://localhost:8761"
echo "   - Gateway: http://localhost:8080"
echo "   - Auth: http://localhost:8082"
echo "   - System: http://localhost:8083"
echo "   - Config: http://localhost:8888"