Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-BetaPath([string]$Root, [string]$Relative) {
    if ([IO.Path]::IsPathRooted($Relative) -or $Relative -match '\\|:|(^|/)\.{1,2}(/|$)' -or $Relative -notmatch '^[A-Za-z0-9][A-Za-z0-9._/ -]*$') { throw 'Unsafe package path.' }
    $base = [IO.Path]::GetFullPath($Root).TrimEnd('\','/') + [IO.Path]::DirectorySeparatorChar
    $path = [IO.Path]::GetFullPath((Join-Path $Root $Relative))
    if (-not $path.StartsWith($base, [StringComparison]::OrdinalIgnoreCase)) { throw 'Package path escapes its folder.' }
    $walk = [IO.Path]::GetFullPath($Root)
    foreach ($part in @('') + ($Relative -split '/')) {
        if ($part) { $walk = Join-Path $walk $part }
        if ((Test-Path -LiteralPath $walk) -and ((Get-Item -LiteralPath $walk -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Linked package folders/files are not supported.' }
    }
    return $path
}

function Read-BetaPackage([string]$Root, [switch]$AllowChangedFiles) {
    $manifestPath = Get-BetaPath $Root 'package.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf) -or (Get-Item -LiteralPath $manifestPath).Length -gt 1048576) { throw 'Missing or oversized package manifest.' }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.format -ne 1 -or $manifest.version -notmatch '^\d+\.\d+\.\d+-beta\.\d+$' -or $manifest.sourceRevision -notmatch '^[a-f0-9]{40}$' -or $manifest.javaMajor -ne 11 -or $manifest.mainClass -ne 'com.fromscratch.beta.BetaLauncher') { throw 'Unsupported or malformed beta package.' }
    if (@($manifest.files).Count -lt 6 -or @($manifest.files).Count -gt 1024) { throw 'Invalid package file list.' }
    $seen = @{}
    foreach ($entry in $manifest.files) {
        $path = Get-BetaPath $Root $entry.path
        if ($seen.ContainsKey($entry.path) -or $entry.sha256 -notmatch '^[a-f0-9]{64}$' -or $entry.bytes -lt 0 -or $entry.bytes -gt 536870912) { throw 'Invalid or duplicate package entry.' }
        $seen[$entry.path] = $true
        if (-not $AllowChangedFiles -and (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -ne $entry.bytes -or (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256)) { throw ('Package verification failed: ' + $entry.path + '. Re-extract a trusted ZIP.') }
    }
    foreach ($required in @('Launch.ps1','Launch.cmd','Uninstall.ps1','Uninstall.cmd','Common.ps1',$manifest.pluginJar)) { if (-not $seen.ContainsKey($required)) { throw 'Incomplete beta package.' } }
    if (-not $AllowChangedFiles) {
        foreach ($jar in Get-ChildItem -LiteralPath (Get-BetaPath $Root 'lib') -Filter '*.jar' -File) {
            if (-not $seen.ContainsKey('lib/' + $jar.Name)) { throw 'An unexpected JAR is present in the launcher classpath. Re-extract a trusted ZIP.' }
        }
    }
    return $manifest
}

function Find-BetaJava {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { $candidates += $command.Source }
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) { $candidates += @(Get-ChildItem -LiteralPath $adoptium -Directory | ForEach-Object { Join-Path $_.FullName 'bin/java.exe' }) }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $previousAction = $ErrorActionPreference
            try { $ErrorActionPreference = 'Continue'; $version = (& $candidate -version 2>&1 | Out-String) } finally { $ErrorActionPreference = $previousAction }
            if ($LASTEXITCODE -eq 0 -and $version -match 'version "11\.') { return [IO.Path]::GetFullPath($candidate) }
        }
    }
    throw 'Java 11 is required. Install the Windows x64 Temurin 11 JDK from https://adoptium.net/temurin/releases/?version=11, then run Install.cmd again. No Git, Gradle or IntelliJ is needed.'
}

function Get-BetaShortcut([string]$Path) {
    $shell = New-Object -ComObject WScript.Shell
    try { return $shell.CreateShortcut($Path) } finally { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell) }
}

function Get-BetaTree([string]$Root) {
    $pending = New-Object 'Collections.Generic.Stack[string]'
    $pending.Push([IO.Path]::GetFullPath($Root))
    $count = 0
    while ($pending.Count -gt 0) {
        foreach ($item in Get-ChildItem -LiteralPath $pending.Pop() -Force) {
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or ++$count -gt 10000) { throw 'Linked or unexpectedly large installation tree. No automatic cleanup is safe.' }
            $item
            if ($item.PSIsContainer) { $pending.Push($item.FullName) }
        }
    }
}

function Test-BetaOwnedShortcut([string]$Shortcut, [string]$InstallRoot) {
    $link = Get-BetaShortcut $Shortcut
    try {
        if ($link.TargetPath -ne (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe')) { return $false }
        if ($link.Arguments -match '-File "([^"\r\n]+)"') {
            $script = $Matches[1]; $base = [IO.Path]::GetFullPath($InstallRoot).TrimEnd('\') + '\'
            if ($script.StartsWith($base, [StringComparison]::OrdinalIgnoreCase) -and (Split-Path $script -Leaf) -eq 'Launch.ps1') {
                return Test-Path -LiteralPath (Join-Path (Split-Path $script) '.from-scratch-beta-owned.json') -PathType Leaf
            }
        }
        return $false
    } finally { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($link) }
}
