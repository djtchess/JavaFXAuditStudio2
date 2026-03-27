param(
    [ValidateSet('all', 'backend', 'frontend')]
    [string]$Target = 'all'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Get-MavenRepoLocal {
    if ($env:MAVEN_REPO_LOCAL) {
        return $env:MAVEN_REPO_LOCAL
    }
    if ($env:USERPROFILE) {
        return (Join-Path $env:USERPROFILE '.m2\repository')
    }
    if ($env:HOME) {
        return (Join-Path $env:HOME '.m2/repository')
    }
    return (Join-Path $root '.m2\repository')
}

function Build-Backend {
    Push-Location (Join-Path $root 'backend')
    try {
        $mavenRepoLocal = Get-MavenRepoLocal
        & ./mvnw.cmd '-q' '-DskipTests' "-Dmaven.repo.local=$mavenRepoLocal" 'package'
    } finally {
        Pop-Location
    }
}

function Build-Frontend {
    Push-Location (Join-Path $root 'frontend')
    try {
        cmd /c npm run build
    } finally {
        Pop-Location
    }
}

if ($Target -eq 'backend' -or $Target -eq 'all') {
    Build-Backend
}

if ($Target -eq 'frontend' -or $Target -eq 'all') {
    Build-Frontend
}
