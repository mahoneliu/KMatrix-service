
# ==========================================
# Run Standalone Docker Container
# ==========================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
Set-Location $ProjectRoot
Write-Host "Working Directory: $ProjectRoot"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting KMatrix Standalone Container" -ForegroundColor Cyan
Write-Host "Standalone Mode: Configs included inside image" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Stop and remove existing container
Write-Host "Stopping existing container (if any)..." -ForegroundColor Yellow
docker stop kmatrix-standalone 2>$null
docker rm kmatrix-standalone 2>$null

# Run new container
Write-Host "Starting new container..." -ForegroundColor Yellow

# Use direct command with line continuation but ENSURE NO TRAILING SPACES
# Or just one long line to be safe
docker run -d --name kmatrix-standalone -p 80:80 -p 8080:8090 -p 5432:5432 -v "${PWD}/kmatrix-data:/kmatrix-data" kmatrix:standalone

if ($?) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Container started successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access URLs:" -ForegroundColor White
    Write-Host "  Frontend:    http://localhost" -ForegroundColor Cyan
    Write-Host "  Backend API: http://localhost:8080" -ForegroundColor Cyan
    Write-Host "  PostgreSQL:  localhost:5432" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "View Logs:" -ForegroundColor White
    Write-Host "  docker logs -f kmatrix-standalone" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Notes:" -ForegroundColor Yellow
    Write-Host "  - All configs are built-in." -ForegroundColor Yellow
    Write-Host "  - To change config, rebuild the image." -ForegroundColor Yellow
    Write-Host "  - Data is persisted in ./kmatrix-data" -ForegroundColor Yellow
    Write-Host ""
}
else {
    Write-Host ""
    Write-Host "Failed to start container!" -ForegroundColor Red
    Write-Host ""
    exit 1
}
