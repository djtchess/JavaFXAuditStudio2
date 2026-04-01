param(
    [ValidateSet('all', 'backend', 'backend-postgres-smoke', 'frontend')]
    [string]$Target = 'all'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Get-MavenRepoLocal {
    if ($env:MAVEN_REPO_LOCAL) {
        return $env:MAVEN_REPO_LOCAL
    }
    return (Join-Path $root '.m2\repository')
}

function Test-Backend {
    Push-Location (Join-Path $root 'backend')
    try {
        $mavenRepoLocal = Get-MavenRepoLocal
        & ./mvnw.cmd "-Dmaven.repo.local=$mavenRepoLocal" 'test'
        if ($LASTEXITCODE -ne 0) {
            throw "La suite backend a echoue avec le code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Test-BackendPostgresSmoke {
    $previousValues = @{
        CI_POSTGRES_ENABLED = [Environment]::GetEnvironmentVariable('CI_POSTGRES_ENABLED', 'Process')
        DB_URL = [Environment]::GetEnvironmentVariable('DB_URL', 'Process')
        DB_USER = [Environment]::GetEnvironmentVariable('DB_USER', 'Process')
        DB_PASSWORD = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'Process')
    }

    Push-Location $root
    try {
        docker compose up -d postgres
        if ($LASTEXITCODE -ne 0) {
            throw "Impossible de demarrer PostgreSQL via Docker Compose."
        }
    } finally {
        Pop-Location
    }

    Push-Location (Join-Path $root 'backend')
    try {
        $mavenRepoLocal = Get-MavenRepoLocal
        $env:CI_POSTGRES_ENABLED = 'true'
        if (-not $env:DB_URL) {
            $env:DB_URL = 'jdbc:postgresql://localhost:5432/javafx_audit'
        }
        if (-not $env:DB_USER) {
            $env:DB_USER = 'javafx_audit'
        }
        if (-not $env:DB_PASSWORD) {
            $env:DB_PASSWORD = 'changeme'
        }
        & ./mvnw.cmd "-Dmaven.repo.local=$mavenRepoLocal" '-Dtest=ff.ss.javaFxAuditStudio.integration.PostgresServiceContainerIT' 'test'
        if ($LASTEXITCODE -ne 0) {
            throw "Le smoke PostgreSQL backend a echoue avec le code $LASTEXITCODE."
        }
    } finally {
        foreach ($entry in $previousValues.GetEnumerator()) {
            if ($null -eq $entry.Value) {
                Remove-Item "Env:$($entry.Key)" -ErrorAction SilentlyContinue
            } else {
                Set-Item "Env:$($entry.Key)" $entry.Value
            }
        }
        Pop-Location
    }
}

function Test-Frontend {
    Push-Location (Join-Path $root 'frontend')
    try {
        cmd /c npm test
        if ($LASTEXITCODE -ne 0) {
            throw "La suite frontend a echoue avec le code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

if ($Target -eq 'backend' -or $Target -eq 'all') {
    Test-Backend
}

if ($Target -eq 'backend-postgres-smoke') {
    Test-BackendPostgresSmoke
}

if ($Target -eq 'frontend' -or $Target -eq 'all') {
    Test-Frontend
}
