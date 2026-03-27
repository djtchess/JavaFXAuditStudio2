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

function Test-Backend {
    Push-Location (Join-Path $root 'backend')
    try {
        $mavenRepoLocal = Get-MavenRepoLocal
        & ./mvnw.cmd "-Dmaven.repo.local=$mavenRepoLocal" 'test'
    } finally {
        Pop-Location
    }
}

function Test-Frontend {
    Push-Location (Join-Path $root 'frontend')
    try {
        cmd /c npm test
    } finally {
        Pop-Location
    }
}

if ($Target -eq 'backend' -or $Target -eq 'all') {
    Test-Backend
}

if ($Target -eq 'frontend' -or $Target -eq 'all') {
    Test-Frontend
}
