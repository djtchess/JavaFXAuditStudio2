param(
    [switch]$SkipFrontendInstall
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$databaseName = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'javafx_audit' }
$databaseUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'javafx_audit' }
$databasePassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'changeme' }
$databaseUrl = if ($env:DB_URL) { $env:DB_URL } else { "jdbc:postgresql://localhost:5432/$databaseName" }

Push-Location $root
try {
    docker compose up -d postgres
} finally {
    Pop-Location
}

if (-not $SkipFrontendInstall) {
    Push-Location $frontendDir
    try {
        cmd /c npm install
    } finally {
        Pop-Location
    }
}

Start-Process powershell -ArgumentList '-NoExit', '-Command', "`$env:DB_URL='$databaseUrl'; `$env:DB_USER='$databaseUser'; `$env:DB_PASSWORD='$databasePassword'; Set-Location '$backendDir'; ./mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$frontendDir'; npm start"
