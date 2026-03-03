# Helper script para ejecutar docker-compose desde la raíz del proyecto
# Uso: .\scripts\docker-helper.ps1 up -d
#      .\scripts\docker-helper.ps1 down
#      .\scripts\docker-helper.ps1 logs -f frontend

$composeFile = "infrastructure/docker/docker-compose.yml"

if (-not (Test-Path $composeFile)) {
    Write-Error "No se encontró el archivo docker-compose.yml en $composeFile"
    exit 1
}

# Pasar todos los argumentos a docker-compose
docker-compose -f $composeFile $args
