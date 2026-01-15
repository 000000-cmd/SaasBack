#!/bin/bash

# ============================================
# SCRIPT DE BACKUP AUTOMÁTICO
# ============================================

set -e

# Configuración
BACKUP_DIR="/opt/saas-backups"
MYSQL_CONTAINER="saas-mysql"
RETENTION_DAYS=7

# Crear directorio de backups
mkdir -p "$BACKUP_DIR"

# Timestamp
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

echo "🗄️  Iniciando backup: $TIMESTAMP"

# Verificar que MySQL está corriendo
if ! docker ps | grep -q "$MYSQL_CONTAINER"; then
    echo "❌ MySQL no está corriendo"
    exit 1
fi

# Backup de MySQL
echo "📦 Backup de base de datos..."
BACKUP_FILE="$BACKUP_DIR/mysql-backup-$TIMESTAMP.sql"
docker exec "$MYSQL_CONTAINER" mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > "$BACKUP_FILE"

if [ -f "$BACKUP_FILE" ]; then
    # Comprimir
    gzip "$BACKUP_FILE"
    echo "✅ Backup creado: ${BACKUP_FILE}.gz"

    # Mostrar tamaño
    SIZE=$(du -h "${BACKUP_FILE}.gz" | cut -f1)
    echo "📊 Tamaño: $SIZE"
else
    echo "❌ Error al crear backup"
    exit 1
fi

# Limpiar backups antiguos
echo "🧹 Limpiando backups antiguos (más de $RETENTION_DAYS días)..."
find "$BACKUP_DIR" -name "mysql-backup-*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "✅ Limpieza completada"

# Listar backups disponibles
echo ""
echo "📋 Backups disponibles:"
ls -lh "$BACKUP_DIR"/mysql-backup-*.sql.gz

echo ""
echo "✅ Backup completado exitosamente"