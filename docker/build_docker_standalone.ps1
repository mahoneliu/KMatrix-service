# ==========================================
# Build Standalone Docker Image
# All configuration files will be packaged into the image
# ==========================================

# Ensure execution in project root directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
Set-Location $ProjectRoot
Write-Host "Working Directory: $ProjectRoot"

Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'Building KMatrix Standalone Docker Image' -ForegroundColor Cyan
Write-Host 'All config files will be packaged into image' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan

# Build the image
docker build -t kmatrix:standalone -f kmatrix-service/docker/Dockerfile.standalone .

# Check if build succeeded
if ($?) {
    Write-Host ''
    Write-Host '========================================' -ForegroundColor Green
    Write-Host 'Build successful!' -ForegroundColor Green
    Write-Host '========================================' -ForegroundColor Green
    Write-Host ''
    Write-Host 'Image name: kmatrix:standalone' -ForegroundColor Yellow
    Write-Host ''
    Write-Host 'Run container:' -ForegroundColor White
    Write-Host '  .\kmatrix-service\docker\run_docker_standalone.ps1' -ForegroundColor Cyan
    Write-Host ''
    Write-Host 'Or run manually:' -ForegroundColor White
    Write-Host '  docker run -d --name kmatrix-standalone -p 80:80 -p 8080:8090 -p 5432:5432 kmatrix:standalone' -ForegroundColor Cyan
    Write-Host ''
}
else {
    Write-Host ''
    Write-Host 'Build failed!' -ForegroundColor Red
    Write-Host ''
    exit 1
}
