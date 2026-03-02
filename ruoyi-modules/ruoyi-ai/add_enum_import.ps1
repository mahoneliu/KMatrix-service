$dir = "f:\code\KMatrix\kmatrix-service\ruoyi-modules\ruoyi-ai\src\main\java\org\dromara\ai\domain\enums"
Get-ChildItem -Path $dir -Filter *.java -Recurse | ForEach-Object {
    $file = $_
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    if ($content -match 'MessageUtils' -and $content -notmatch 'import org.dromara.common.core.utils.MessageUtils;') {
        
        # Try to insert after the package declaration
        if ($content -match '(package\s+[^;]+;)') {
            $pkg = $matches[1]
            $replacement = "$pkg`r`n`r`nimport org.dromara.common.core.utils.MessageUtils;"
            $content = $content.Replace($pkg, $replacement)
            
            # Save using utf8 without BOM
            $utf8bom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($file.FullName, $content, $utf8bom)
            Write-Host "Added import to: $($file.Name)"
        }
    }
}
