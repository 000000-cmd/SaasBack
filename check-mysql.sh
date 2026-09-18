#!/bin/bash

# ============================================
# SCRIPT PARA VERIFICAR MYSQL
# ============================================

echo "🔍 Verificando conexión a MySQL..."

# Verificar si el contenedor está corriendo
if ! docker ps | grep -q saas-mysql; then
    echo "❌ MySQL no está corriendo"
    exit 1
fi

echo "✅ Contenedor MySQL está corriendo"

# Verificar conexión
echo ""
echo "🔐 Probando conexión con usuario root..."
docker exec saas-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 'Conexión exitosa' AS status;"

if [ $? -eq 0 ]; then
    echo "✅ Conexión exitosa"
else
    echo "❌ Error de conexión"
    exit 1
fi

# Verificar base de datos
echo ""
echo "📊 Verificando bases de datos..."
docker exec saas-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"

echo ""
echo "👥 Verificando usuarios..."
docker exec saas-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT User, Host FROM mysql.user;"

echo ""
echo "✅ Verificación completada"