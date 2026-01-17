#!/bin/bash

# ============================================
# SCRIPT DE BACKUP AUTOMATICO
# SAAS Platform - Linea Base
# ============================================
# Uso:
#   ./backup.sh              # Backup completo
#   ./backup.sh --restore    # Listar y restaurar backups
#   ./backup.sh --list       # Listar backups disponibles
# ============================================

set -euo pipefail

# ============================================
# CONFIGURACION
# ============================================
BACKUP_DIR="/opt/saas-backups"
MYSQL_CONTAINER="saas-mysql"
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Cargar variables de entorno si existe .env
if [ -f /opt/saas-platform/.env ]; then
    set -a
    # shellcheck source=/dev/null
    source /opt/saas-platform/.env
    set +a
fi

# ============================================
# COLORES
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================
# FUNCIONES DE LOGGING
# ============================================
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ADVERTENCIA:${NC} $1"
}

info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

# ============================================
# FUNCION: CREAR BACKUP
# ============================================
create_backup() {
    log "Iniciando backup: $TIMESTAMP"

    # Crear directorio si no existe
    mkdir -p "$BACKUP_DIR"

    # Verificar que MySQL esta corriendo
    if ! docker ps 2>/dev/null | grep -q "$MYSQL_CONTAINER"; then
        error "MySQL no esta corriendo"
        exit 1
    fi

    # Verificar conexion
    if ! docker exec "$MYSQL_CONTAINER" mysqladmin ping -h localhost -uroot -p"${MYSQL_ROOT_PASSWORD}" &>/dev/null; then
        error "No se puede conectar a MySQL"
        exit 1
    fi

    BACKUP_FILE="$BACKUP_DIR/mysql-backup-$TIMESTAMP.sql"

    log "Creando backup de base de datos..."

    # Crear backup con opciones optimizadas
    if docker exec "$MYSQL_CONTAINER" mysqldump \
        -uroot \
        -p"${MYSQL_ROOT_PASSWORD}" \
        --all-databases \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --routines \
        --triggers \
        --events \
        > "$BACKUP_FILE" 2>/dev/null; then

        # Comprimir
        gzip "$BACKUP_FILE"

        # Mostrar resultado
        local size
        size=$(du -h "${BACKUP_FILE}.gz" | cut -f1)
        log "Backup creado exitosamente: ${BACKUP_FILE}.gz ($size)"

        # Limpiar backups antiguos
        cleanup_old_backups

        return 0
    else
        error "Error al crear backup"
        rm -f "$BACKUP_FILE" 2>/dev/null
        return 1
    fi
}

# ============================================
# FUNCION: LIMPIAR BACKUPS ANTIGUOS
# ============================================
cleanup_old_backups() {
    log "Limpiando backups antiguos (mas de $RETENTION_DAYS dias)..."

    local deleted=0
    while IFS= read -r -d '' file; do
        rm -f "$file"
        deleted=$((deleted + 1))
    done < <(find "$BACKUP_DIR" -name "mysql-backup-*.sql.gz" -mtime +$RETENTION_DAYS -print0 2>/dev/null)

    if [ $deleted -gt 0 ]; then
        info "Eliminados $deleted backups antiguos"
    else
        info "No hay backups antiguos para eliminar"
    fi
}

# ============================================
# FUNCION: LISTAR BACKUPS
# ============================================
list_backups() {
    log "Backups disponibles en $BACKUP_DIR:"
    echo ""

    if [ ! -d "$BACKUP_DIR" ]; then
        warning "Directorio de backups no existe"
        return 1
    fi

    local count=0
    while IFS= read -r file; do
        local size date_modified
        size=$(du -h "$file" | cut -f1)
        date_modified=$(stat -c %y "$file" 2>/dev/null | cut -d'.' -f1 || stat -f %Sm "$file" 2>/dev/null)

        count=$((count + 1))
        printf "  %2d. %-50s %8s  %s\n" "$count" "$(basename "$file")" "$size" "$date_modified"
    done < <(ls -t "$BACKUP_DIR"/mysql-backup-*.sql.gz 2>/dev/null)

    if [ $count -eq 0 ]; then
        warning "No hay backups disponibles"
        return 1
    fi

    echo ""
    info "Total: $count backups"
}

# ============================================
# FUNCION: RESTAURAR BACKUP
# ============================================
restore_backup() {
    log "Iniciando proceso de restauracion..."
    echo ""

    # Listar backups disponibles
    list_backups || exit 1

    echo ""
    read -rp "Ingresa el numero del backup a restaurar (0 para cancelar): " selection

    if [ "$selection" = "0" ]; then
        info "Restauracion cancelada"
        exit 0
    fi

    # Obtener archivo seleccionado
    local backup_file
    backup_file=$(ls -t "$BACKUP_DIR"/mysql-backup-*.sql.gz 2>/dev/null | sed -n "${selection}p")

    if [ -z "$backup_file" ] || [ ! -f "$backup_file" ]; then
        error "Seleccion invalida"
        exit 1
    fi

    echo ""
    warning "ATENCION: Esto reemplazara TODOS los datos actuales"
    warning "Archivo: $backup_file"
    echo ""
    read -rp "Escribir 'CONFIRMAR' para continuar: " confirm

    if [ "$confirm" != "CONFIRMAR" ]; then
        info "Restauracion cancelada"
        exit 0
    fi

    log "Restaurando desde: $backup_file"

    # Verificar que MySQL esta corriendo
    if ! docker ps 2>/dev/null | grep -q "$MYSQL_CONTAINER"; then
        error "MySQL no esta corriendo"
        exit 1
    fi

    # Descomprimir y restaurar
    if gunzip -c "$backup_file" | docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" 2>/dev/null; then
        log "Restauracion completada exitosamente"

        # Reiniciar servicios para que reconecten
        warning "Es recomendable reiniciar los servicios: docker compose restart"
    else
        error "Error durante la restauracion"
        exit 1
    fi
}

# ============================================
# FUNCION: MOSTRAR AYUDA
# ============================================
show_help() {
    echo "Uso: $0 [opcion]"
    echo ""
    echo "Opciones:"
    echo "  (sin argumentos)   Crear backup de MySQL"
    echo "  --restore          Restaurar desde un backup"
    echo "  --list             Listar backups disponibles"
    echo "  --cleanup          Limpiar backups antiguos"
    echo "  --help, -h         Mostrar esta ayuda"
    echo ""
    echo "Variables de entorno:"
    echo "  BACKUP_RETENTION_DAYS   Dias que se mantienen los backups (default: 7)"
    echo "  MYSQL_ROOT_PASSWORD     Password de MySQL (requerido)"
    echo ""
}

# ============================================
# MAIN
# ============================================
case "${1:-}" in
    --restore|-r)
        restore_backup
        ;;
    --list|-l)
        list_backups
        ;;
    --cleanup|-c)
        cleanup_old_backups
        ;;
    --help|-h)
        show_help
        ;;
    "")
        create_backup
        ;;
    *)
        error "Opcion desconocida: $1"
        show_help
        exit 1
        ;;
esac