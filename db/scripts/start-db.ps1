# ============================================================
#  start-db.ps1 — Start PostgreSQL and apply pending init SQLs
#
#  Usage:
#    .\db\scripts\start-db.ps1
# ============================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Join-Path $PSScriptRoot "..\.."
Push-Location $projectRoot

try {
    Write-Host ""
    Write-Host "Starting PostgreSQL container..." -ForegroundColor Cyan
    docker compose up -d postgres | Out-Null

    Write-Host "Applying pending database scripts..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "apply-pending-init.ps1")

    Write-Host "Database is ready." -ForegroundColor Green
    Write-Host ""
}
finally {
    Pop-Location
}

