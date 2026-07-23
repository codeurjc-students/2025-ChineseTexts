#!/bin/bash
set -e

# === ChineseReads — restauración / simulacro de backup de MySQL ===
#
# Dos modos:
#
#   ./restore-db.sh --drill <fichero.sql.gz>
#       SIMULACRO (seguro, no toca la base de datos real): restaura el backup
#       en una base de datos temporal, cuenta tablas y filas clave para
#       comprobar que el backup es restaurable, y borra la base temporal.
#       Hazlo al menos una vez con un backup real — un backup sin simulacro
#       de restauración es solo una esperanza.
#
#   ./restore-db.sh <fichero.sql.gz>
#       RESTAURACIÓN REAL: SOBRESCRIBE la base de datos de producción con el
#       contenido del backup. Pide confirmación escribiendo RESTAURAR.
#       Solo para desastres reales.
#
# Configurable por entorno: MYSQL_ROOT_PASSWORD (password), DB_NAME (chinesereads).

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-password}"
DB_NAME="${DB_NAME:-chinesereads}"
DRILL_DB="${DB_NAME}_drill"

MODE="real"
if [ "$1" = "--drill" ]; then
    MODE="drill"
    shift
fi

FILE="$1"
if [ -z "$FILE" ] || [ ! -f "$FILE" ]; then
    echo "Uso: $0 [--drill] <fichero.sql.gz>" >&2
    exit 1
fi

mysql_exec() {
    docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db mysql -uroot "$@"
}

if [ "$MODE" = "drill" ]; then
    echo "=== SIMULACRO de restauración (no toca '$DB_NAME') ==="
    echo "Restaurando $FILE en la base temporal '$DRILL_DB'..."
    mysql_exec -e "DROP DATABASE IF EXISTS \`$DRILL_DB\`; CREATE DATABASE \`$DRILL_DB\` CHARACTER SET utf8mb4;"
    gunzip -c "$FILE" | mysql_exec "$DRILL_DB"

    echo ""
    echo "Comprobación del contenido restaurado:"
    mysql_exec -e "SELECT COUNT(*) AS tablas FROM information_schema.tables WHERE table_schema='$DRILL_DB';"
    mysql_exec "$DRILL_DB" -e "SELECT (SELECT COUNT(*) FROM user) AS usuarios, (SELECT COUNT(*) FROM text) AS textos, (SELECT COUNT(*) FROM word) AS palabras;" \
        || echo "(alguna tabla clave no existe — revisa el backup)"

    mysql_exec -e "DROP DATABASE \`$DRILL_DB\`;"
    echo ""
    echo "OK: el backup es restaurable. Base temporal eliminada."
    exit 0
fi

echo "=== RESTAURACIÓN REAL ==="
echo "Esto SOBRESCRIBIRÁ la base de datos '$DB_NAME' con: $FILE"
echo "Todos los datos posteriores a ese backup se PERDERÁN."
read -r -p "Escribe RESTAURAR para continuar: " CONFIRM
if [ "$CONFIRM" != "RESTAURAR" ]; then
    echo "Cancelado."
    exit 1
fi

echo "Restaurando..."
mysql_exec -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4;"
gunzip -c "$FILE" | mysql_exec "$DB_NAME"
echo "OK: restauración completada. Reinicia el backend para renovar conexiones:"
echo "  docker compose --env-file .env restart backend"
