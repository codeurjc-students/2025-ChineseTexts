#!/bin/bash
set -e

# Desactiva la telemetría del Angular CLI de forma no interactiva, en cualquier
# máquina. Evita que `ng build` pregunte y escriba `cli.analytics` en angular.json
# (lo que "ensuciaría" el repo y haría fallar el `git pull` del siguiente deploy).
# OJO: esto NO afecta a Google Analytics de la web; solo a la telemetría del CLI.
export NG_CLI_ANALYTICS=false

echo "=== ChineseReads Deploy ==="

# 1. Build frontend
echo "Building Angular frontend..."
cd ../frontend
npm install
npm run build
cd ../docker

# 2. Build y arrancar todo con Docker Compose
echo "Starting all services..."
docker compose --env-file .env up -d --build

# 2b. Recargar Caddy. El Caddyfile va bind-mounted, así que `up -d` no reinicia el
#     contenedor cuando solo cambia su contenido; sin esto, los cambios del Caddyfile
#     no se aplican. `caddy reload` es sin cortes; si falla, reinicio el contenedor.
echo "Reloading Caddy configuration..."
docker compose --env-file .env exec caddy caddy reload --config /etc/caddy/Caddyfile 2>/dev/null \
  || docker compose --env-file .env restart caddy

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