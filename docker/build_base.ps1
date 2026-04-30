$ErrorActionPreference = "Stop"

# Image name and tag
$IMAGE_NAME = "registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix_base"
$IMAGE_TAG = "latest"

# Change to docker directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "Starting to build base image: ${IMAGE_NAME}:${IMAGE_TAG}" -ForegroundColor Cyan
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" -f Dockerfile.base .

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build success!" -ForegroundColor Green
    Write-Host "Pushing to Aliyun registry..." -ForegroundColor Cyan
    
    docker push "${IMAGE_NAME}:${IMAGE_TAG}"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Push success: ${IMAGE_NAME}:${IMAGE_TAG}" -ForegroundColor Green
    } else {
        Write-Host "Push failed. Please check network or login status." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Build failed. Please check Dockerfile.base and build process." -ForegroundColor Red
    exit 1
}
