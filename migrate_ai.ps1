$ErrorActionPreference = "Stop"

$baseDir = "f:\code\KMatrix-community\kmatrix-service\ruoyi-modules"
$oldAiDir = "$baseDir\ruoyi-ai\src\main\java\org\dromara\ai"
$oldAiResDir = "$baseDir\ruoyi-ai\src\main\resources"

# Define module mappings (ClassName/Pattern -> Module Name)
$moduleMap = @(
    # == WORKFLOW ==
    @{ Pattern = ".*Workflow.*"; Module = "workflow" },
    @{ Pattern = ".*Node.*"; Module = "workflow" },
    @{ Pattern = ".*LangGraph.*"; Module = "workflow" },
    
    # == KNOWLEDGE ==
    @{ Pattern = ".*Knowledge.*"; Module = "knowledge" },
    @{ Pattern = ".*Dataset.*"; Module = "knowledge" },
    @{ Pattern = ".*Document.*"; Module = "knowledge" },
    @{ Pattern = ".*Chunk.*"; Module = "knowledge" },
    @{ Pattern = ".*Embedding.*"; Module = "knowledge" },
    @{ Pattern = ".*DataSource.*"; Module = "knowledge" },
    @{ Pattern = ".*DatabaseMeta.*"; Module = "knowledge" },
    @{ Pattern = ".*TempFile.*"; Module = "knowledge" },
    @{ Pattern = ".*Question.*"; Module = "knowledge" },
    @{ Pattern = ".*Retrieval.*"; Module = "knowledge" },
    @{ Pattern = ".*Rerank.*"; Module = "knowledge" },
    @{ Pattern = ".*Etl.*"; Module = "knowledge" },
    @{ Pattern = ".*Tika.*"; Module = "knowledge" },
    @{ Pattern = ".*Ddl.*"; Module = "knowledge" },
    @{ Pattern = ".*Embed.*"; Module = "knowledge" },

    # == MODEL ==
    @{ Pattern = ".*Model.*"; Module = "model" },
    @{ Pattern = ".*AiConfig.*"; Module = "model" },
    @{ Pattern = ".*KmAiProperties.*"; Module = "model" },
    
    # == APP (Everything else mostly falls here, but let's be explicit) ==
    @{ Pattern = ".*App.*"; Module = "app" },
    @{ Pattern = ".*Chat.*"; Module = "app" },
    @{ Pattern = ".*Skill.*"; Module = "app" },
    @{ Pattern = ".*BuiltinTool.*"; Module = "app" },
    @{ Pattern = ".*McpServer.*"; Module = "app" },
    @{ Pattern = ".*Token.*"; Module = "app" }
)

$allOldFiles = Get-ChildItem -Path $oldAiDir -Recurse -Filter "*.java" | Where-Object { $_.FullName -notmatch "enum" -and $_.FullName -notmatch "config\\App.*Config|ParamDefinition" }

$replacements = @{} # ClassName -> NewPackage
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)

Write-Host "Determining new location for each file..."
foreach ($file in $allOldFiles) {
    $relPath = $file.FullName.Substring($oldAiDir.Length + 1)
    $fileName = $file.Name
    $className = $file.BaseName
    
    if ($className -in @("SseEventType", "ChatUserType", "AppModelConfig", "AppKnowledgeConfig", "AppWorkflowConfig", "AppParametersConfig", "ParamDefinition")) {
        continue;
    }

    $targetModule = "app"
    if ($relPath -match "^handler\\") { $targetModule = "app" }
    elseif ($relPath -match "^auth\\") { $targetModule = "app" }
    elseif ($relPath -match "^task\\") { $targetModule = "app" }
    elseif ($relPath -match "^workflow\\") { $targetModule = "workflow" }
    elseif ($relPath -match "^service\\etl\\") { $targetModule = "knowledge" }
    else {
        foreach ($map in $moduleMap) {
            if ($fileName -match $map.Pattern) {
                $targetModule = $map.Module
                break
            }
        }
    }
    
    $oldPackage = (Get-Content $file.FullName -Encoding UTF8 | Select-Object -First 20 | Where-Object { $_ -match "^package org\.dromara\.ai" }) -replace "^package\s+(.*?);.*", "`$1"
    
    if (-not $oldPackage) {
        $oldPackage = "org.dromara.ai." + ($relPath.Replace("\", "/").Substring(0, $relPath.LastIndexOf("\")).Replace("/", "."))
    }
    $oldPackage = $oldPackage.Trim()
    
    $newPackage = $oldPackage -replace "^org\.dromara\.ai", "org.dromara.ai.$targetModule"
    
    $destRoot = "$baseDir\ruoyi-ai-$targetModule\src\main\java\org\dromara\ai\$targetModule"
    $destRelPath = $relPath
    $destFile = Join-Path $destRoot $destRelPath
    
    $destDir = Split-Path $destFile -Parent
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir | Out-Null }
    
    Copy-Item -Path $file.FullName -Destination $destFile -Force
    
    $oldFqcn = "$oldPackage.$className"
    $newFqcn = "$newPackage.$className"
    
    $replacements[$oldFqcn] = $newFqcn
}

Write-Host "Updating local package statements in migrated files..."
$newModules = @("ruoyi-ai-model", "ruoyi-ai-knowledge", "ruoyi-ai-workflow", "ruoyi-ai-app")
foreach ($mod in $newModules) {
    if (Test-Path "$baseDir\$mod\src\main\java") {
        $modFiles = Get-ChildItem -Path "$baseDir\$mod\src\main\java" -Recurse -Filter "*.java"
        foreach ($f in $modFiles) {
            $content = [System.IO.File]::ReadAllText($f.FullName, $utf8NoBom)
            $rel = $f.FullName.Substring($f.FullName.IndexOf("src\main\java\") + 14)
            $expectedPackage = (Split-Path $rel -Parent).Replace("\", ".")
            $content = $content -replace "(?m)^package\s+org\.dromara\.ai.*?;", "package $expectedPackage;"
            [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        }
    }
}

Write-Host "Migrating Mapper XML files..."
$xmlFiles = Get-ChildItem -Path "$oldAiResDir\mapper\ai" -Filter "*.xml"
foreach ($xml in $xmlFiles) {
    $targetModule = "app"
    foreach ($map in $moduleMap) {
        if ($xml.Name -match $map.Pattern) {
            $targetModule = $map.Module
            break
        }
    }
    
    $destDir = "$baseDir\ruoyi-ai-$targetModule\src\main\resources\mapper\ai"
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $xml.FullName -Destination "$destDir\$($xml.Name)" -Force
    
    $xmlContent = [System.IO.File]::ReadAllText("$destDir\$($xml.Name)", $utf8NoBom)
    foreach ($entry in $replacements.GetEnumerator()) {
        if ($xmlContent.Contains($entry.Key)) {
            $xmlContent = $xmlContent.Replace($entry.Key, $entry.Value)
        }
    }
    [System.IO.File]::WriteAllText("$destDir\$($xml.Name)", $xmlContent, $utf8NoBom)
}

Write-Host "Updating global references and imports in ruoyi-modules..."
$allJavaFilesToFix = Get-ChildItem -Path $baseDir -Recurse -Filter "*.java" | Where-Object { $_.FullName -notmatch "\\ruoyi-ai\\" }
$adminJavaFiles = Get-ChildItem -Path "f:\code\KMatrix-community\kmatrix-service\ruoyi-admin" -Recurse -Filter "*.java"

$allJavaToFix = $allJavaFilesToFix + $adminJavaFiles

foreach ($jf in $allJavaToFix) {
    $content = [System.IO.File]::ReadAllText($jf.FullName, $utf8NoBom)
    $modified = $false
    
    foreach ($entry in $replacements.GetEnumerator()) {
        if ($content.Contains($entry.Key)) {
            $content = $content.Replace($entry.Key, $entry.Value)
            $content = $content.Replace("import " + $entry.Key + ";", "import " + $entry.Value + ";")
            $modified = $true
        }
    }
    
    if ($content.Contains("org.dromara.ai.domain.enums.ChatUserType")) {
        $content = $content.Replace("org.dromara.ai.domain.enums.ChatUserType", "org.dromara.ai.api.enums.ChatUserType")
        $modified = $true
    }
    if ($content.Contains("org.dromara.ai.domain.enums.SseEventType")) {
        $content = $content.Replace("org.dromara.ai.domain.enums.SseEventType", "org.dromara.ai.api.enums.SseEventType")
        $modified = $true
    }
    if ($content -match "org\.dromara\.ai\.domain\.vo\.config") {
        $content = $content -replace "org\.dromara\.ai\.domain\.vo\.config", "org.dromara.ai.api.domain.vo.config"
        $modified = $true
    }
    
    if ($modified) {
        [System.IO.File]::WriteAllText($jf.FullName, $content, $utf8NoBom)
    }
}

Write-Host "Removing old ruoyi-ai directory completely..."
Remove-Item -Path "$baseDir\ruoyi-ai" -Recurse -Force

Write-Host "Migration script completed successfully."
