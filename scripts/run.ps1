param(
    [switch]$Detached
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Push-Location $root
try {
    if ($Detached) {
        docker compose up --build -d
    } else {
        docker compose up --build
    }
} finally {
    Pop-Location
}
