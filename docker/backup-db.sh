#!/bin/bash
set -e

# === ChineseReads — backup de MySQL ===
#
# Vuelca la base de datos del contenedor `db` a un .sql.gz fechado, verifica
# que el volcado está completo y borra los backups más antiguos que
# RETENTION_DAYS. Pensado para ejecutarse desde este directorio (docker/),
# a mano o desde cron.
#
# Instalación en el VPS (una vez):
#   crontab -e
#   15 4 * * * cd /ruta/al/repo/docker && ./backup-db.sh >> $HOME/backups/chinesereads/backup.log 2>&1
#
# IMPORTANTE: un backup que solo vive en el propio servidor no protege contra
# la pérdida del servidor. Copia fuera periódicamente, por ejemplo desde tu
# ordenador personal:
#   scp 'usuario@servidor:~/backups/chinesereads/*.sql.gz' ~/backups-chinesereads/
# (o automatízalo con rclone hacia un almacenamiento en la nube).
#
# Configurable por variables de entorno (valores por defecto entre paréntesis):
#   BACKUP_DIR      destino de los backups   ($HOME/backups/chinesereads)
#   RETENTION_DAYS  días que se conservan    (14)
#   MYSQL_ROOT_PASSWORD  contraseña de root  (password — la del compose)
#   DB_NAME         base de datos            (chinesereads)

BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/chinesereads}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-password}"
DB_NAME="${DB_NAME:-chinesereads}"

STAMP="$(date +%Y-%m-%d_%H%M%S)"
FILE="$BACKUP_DIR/${DB_NAME}-${STAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backing up '$DB_NAME' to $FILE"

# --single-transaction: volcado consistente sin bloquear la app (InnoDB).
# --routines --triggers --events: incluye todo lo que no son filas.
# -T: sin TTY (necesario bajo cron). La contraseña va por variable de entorno
#     al contenedor para que no aparezca en `ps` del host.
docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
    mysqldump --single-transaction --quick --routines --triggers --events \
    -uroot "$DB_NAME" | gzip > "$FILE"

# Verificación: el gzip es válido Y el volcado termina con el pie de mysqldump
# ("Dump completed"). Un dump cortado a mitad (disco lleno, contenedor caído)
# no lo incluye, y es mejor enterarse hoy que el día que haga falta restaurar.
if ! gzip -t "$FILE" 2>/dev/null; then
    echo "ERROR: el backup no es un gzip válido: $FILE" >&2
    rm -f "$FILE"
    exit 1
fi
if ! gunzip -c "$FILE" | tail -n 5 | grep -q "Dump completed"; then
    echo "ERROR: el volcado está incompleto (falta 'Dump completed'): $FILE" >&2
    rm -f "$FILE"
    exit 1
fi

SIZE="$(du -h "$FILE" | cut -f1)"
echo "OK: backup verificado ($SIZE)"

# Rotación: borra los backups de esta base de datos más antiguos que N días.
DELETED="$(find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime "+$RETENTION_DAYS" -print -delete | wc -l | tr -d ' ')"
if [ "$DELETED" != "0" ]; then
    echo "Rotación: eliminados $DELETED backups con más de $RETENTION_DAYS días"
fi

echo "Backups actuales en $BACKUP_DIR:"
ls -lh "$BACKUP_DIR" | grep "${DB_NAME}-" | tail -n 5
