#!/bin/bash
set -e

# Desactiva la telemetría del Angular CLI de forma no interactiva, en cualquier
# máquina. Evita que `ng build` pregunte y escriba `cli.analytics` en angular.json
# (lo que "ensuciaría" el repo y haría fallar el `git pull` del siguiente deploy).
# OJO: esto NO afecta a Google Analytics de la web; solo a la telemetría del CLI.
export NG_CLI_ANALYTICS=false

echo "=== ChineseReads Deploy ==="

# 0. Red de seguridad de la base de datos, sin pasos manuales:
#    a) Backup PREVIO al despliegue: si la BD está en marcha, se vuelca antes de
#       tocar nada. Si este despliegue rompiera algo (p. ej. un cambio de
#       esquema), existe una foto de justo antes. Si el backup falla, el deploy
#       SE ABORTA: desplegar sin red es exactamente lo que queremos evitar.
#    b) Alta (idempotente) del backup diario en cron: cada madrugada a las
#       04:15 se guarda una copia; la rotación conserva los últimos 14 días.
#       Se instala sola en el primer deploy; los siguientes no la duplican.
if [ -n "$(docker compose --env-file .env ps -q --status running db 2>/dev/null)" ]; then
    echo "Pre-deploy database backup..."
    ./backup-db.sh
else
    echo "(db container not running yet - skipping pre-deploy backup)"
fi

DOCKER_DIR="$(pwd)"
if ! crontab -l 2>/dev/null | grep -qF "backup-db.sh"; then
    ( crontab -l 2>/dev/null; \
      echo "15 4 * * * mkdir -p \$HOME/backups/chinesereads && cd $DOCKER_DIR && ./backup-db.sh >> \$HOME/backups/chinesereads/backup.log 2>&1" \
    ) | crontab -
    echo "Installed daily 04:15 database backup in cron"
fi

# 1. Build frontend
echo "Building Angular frontend..."
cd ../frontend
npm install
npm run build
cd ../docker

# 2. Build y arrancar todo con Docker Compose
echo "Starting all services..."
docker compose --env-file .env up -d --build

# 2b. Recrear Caddy para que tome el Caddyfile actualizado. El Caddyfile va
#     bind-mounted como fichero ÚNICO: al hacer `git pull` se sustituye por un
#     inodo nuevo, pero el contenedor en marcha sigue apuntando al inodo viejo,
#     así que ni `up -d` ni `restart` aplican los cambios. `--force-recreate`
#     vuelve a montar el fichero actual; `--no-deps` evita tocar backend/db.
echo "Recreating Caddy to pick up Caddyfile changes..."
docker compose --env-file .env up -d --force-recreate --no-deps caddy

# 2c. Recrear el contenedor SSR por el mismo motivo: monta ../frontend/dist/frontend
#     y `npm run build` regenera ese directorio, así que el contenedor en marcha
#     puede quedarse apuntando a inodos viejos del build anterior.
echo "Recreating frontend-ssr to pick up the new build..."
docker compose --env-file .env up -d --force-recreate --no-deps frontend-ssr

# 3. Reclamar espacio: caché de build + imágenes huérfanas (versiones antiguas
#    de backend/frontend que quedan sin etiqueta tras el --build).
#    Seguro: no toca imágenes en uso, contenedores ni volúmenes (la BD MySQL).
echo "Reclaiming unused Docker space..."
docker builder prune -f || true
docker image prune -f || true

echo ""
echo "=== Deploy complete ==="
echo "App running at https://chinesereads.com"
echo ""
echo "Useful commands:"
echo "  docker compose logs -f backend    # Ver logs del backend"
echo "  docker compose logs -f ai-service # Ver logs de la IA"
echo "  docker compose down               # Parar todo"