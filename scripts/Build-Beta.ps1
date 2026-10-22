[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') betaChecksum --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Beta packaging failed. No candidate should be distributed.' }
    Write-Host 'Candidate ZIP and SHA-256 are in build/distributions. This did not publish or install anything.'
} finally { Pop-Location }
