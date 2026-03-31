param(
    [switch]$RestartPostgres
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Push-Location $root
try {
    docker compose down --volumes --remove-orphans
    if ($RestartPostgres) {
        docker compose up -d postgres
    }
} finally {
    Pop-Location
}
