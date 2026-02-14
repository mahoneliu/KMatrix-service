# Stop and remove existing container
# 确保在项目根目录下执行以正确挂载数据卷
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
Set-Location $ProjectRoot
Write-Host "Working Directory: $ProjectRoot"

docker stop kmatrix-container
docker rm kmatrix-container

# Run new container
# 注意: 配置文件路径已更新为 kmatrix-service/docker/...
docker run -d `
    --name kmatrix-container `
    -p 80:80 `
    -p 8080:8090 `
    -p 5432:5432 `
    -v ${PWD}/kmatrix-data:/kmatrix-data `
    kmatrix

Write-Host "KMatrix container started. Access at http://localhost"
