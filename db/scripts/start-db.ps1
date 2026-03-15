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
    Write-Host "Starting PostgreSQL and migration services..." -ForegroundColor Cyan
    docker compose up -d postgres db-migrator | Out-Null

    Write-Host "Database is ready." -ForegroundColor Green
    Write-Host ""
}
finally {
    Pop-Location
}

