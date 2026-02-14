# Build the image

# Ensure execution in project root directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
Set-Location $ProjectRoot
Write-Host "Working Directory: $ProjectRoot"

Write-Host 'Building KMatrix Docker Image...'
docker build -t kmatrix -f kmatrix-service/docker/Dockerfile .

# Check if build succeeded
if ($?) {
    Write-Host 'Build successful!' -ForegroundColor Green
    Write-Host 'To run the container:'
    Write-Host '  .\kmatrix-service\docker\run_docker.ps1'
}
else {
    Write-Host 'Build failed!' -ForegroundColor Red
    exit 1
}
