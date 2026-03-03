#!/bin/bash
# Helper script para ejecutar docker-compose desde la raíz del proyecto
# Uso: ./scripts/docker-helper.sh up -d
#      ./scripts/docker-helper.sh down
#      ./scripts/docker-helper.sh logs -f frontend

COMPOSE_FILE="infrastructure/docker/docker-compose.yml"

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "Error: No se encontró el archivo docker-compose.yml en $COMPOSE_FILE"
    exit 1
fi

# Pasar todos los argumentos a docker-compose
docker-compose -f "$COMPOSE_FILE" "$@"
