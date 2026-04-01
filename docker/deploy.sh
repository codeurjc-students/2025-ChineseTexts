#!/bin/bash
set -e

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

echo ""
echo "=== Deploy complete ==="
echo "App running at https://chinesereads.com"
echo ""
echo "Useful commands:"
echo "  docker compose logs -f backend    # Ver logs del backend"
echo "  docker compose logs -f ai-service # Ver logs de la IA"
echo "  docker compose down               # Parar todo"