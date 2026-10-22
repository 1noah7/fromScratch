[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    & .\gradlew.bat previewIconIds --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Could not prepare the sample interface.' }
    $icons = Join-Path $projectRoot 'build/marketing/icons'
    New-Item -ItemType Directory -Path $icons -Force | Out-Null
    foreach ($id in Get-Content 'build/marketing/item-ids.txt') {
        $path = Join-Path $icons "$id.png"
        if (!(Test-Path -LiteralPath $path)) {
            Invoke-WebRequest "https://static.runelite.net/cache/item/icon/$id.png" -UseBasicParsing -OutFile $path
        }
    }
    & .\gradlew.bat renderUiPreviews --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Interface rendering failed. Review the error before using the images.' }
    Write-Host 'Actual Swing components with sample data rendered to docs/media. No game or player profile was opened.'
} finally { Pop-Location }
