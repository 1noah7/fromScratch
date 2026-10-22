[CmdletBinding()]
param(
    [string]$InstallRoot = (Join-Path $env:LOCALAPPDATA 'FromScratchBeta'),
    [string]$DesktopDirectory = [Environment]::GetFolderPath('Desktop')
)
. (Join-Path $PSScriptRoot 'Common.ps1')
$manifest = Read-BetaPackage $PSScriptRoot
[void](Find-BetaJava)
$InstallRoot = [IO.Path]::GetFullPath($InstallRoot).TrimEnd('\')
if ($InstallRoot -eq [IO.Path]::GetPathRoot($InstallRoot).TrimEnd('\') -or $InstallRoot -match '(?i)(^|[\\/])\.runelite([\\/]|$)') { throw 'Choose a separate, user-owned beta program folder.' }
$hash = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'package.json') -Algorithm SHA256).Hash.ToLowerInvariant()
$target = Get-BetaPath $InstallRoot ('versions/' + $manifest.version + '-' + $hash.Substring(0,12))
$shortcut = Join-Path $DesktopDirectory 'From Scratch Beta.lnk'
if ((Test-Path -LiteralPath $shortcut) -and -not (Test-BetaOwnedShortcut $shortcut $InstallRoot)) { throw 'A different desktop shortcut already uses From Scratch Beta. It was preserved; choose another desktop folder or rename it yourself.' }
$stage = $null
$temporaryShortcut = $null
try {
    if (Test-Path -LiteralPath $target) {
        $stampPath = Join-Path $target '.from-scratch-beta-owned.json'
        if (-not (Test-Path -LiteralPath $stampPath)) { throw 'The destination is not a recognized beta installation. Nothing was overwritten.' }
        $stamp = Get-Content -LiteralPath $stampPath -Raw | ConvertFrom-Json
        if ($stamp.manifestSha256 -ne $hash) { throw 'Destination ownership does not match this package.' }
        [void](Read-BetaPackage $target)
    } else {
        [void](New-Item -ItemType Directory -Path (Split-Path $target) -Force)
        $stage = Get-BetaPath $InstallRoot ('versions/staging-' + [Guid]::NewGuid().ToString('N'))
        [void](New-Item -ItemType Directory -Path $stage)
        foreach ($entry in $manifest.files) {
            $destination = Get-BetaPath $stage $entry.path
            [void](New-Item -ItemType Directory -Path (Split-Path $destination) -Force)
            Copy-Item -LiteralPath (Get-BetaPath $PSScriptRoot $entry.path) -Destination $destination
        }
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'package.json') -Destination (Join-Path $stage 'package.json')
        [void](Read-BetaPackage $stage)
        [ordered]@{format=1;directory=$target;installRoot=$InstallRoot;desktop=$DesktopDirectory;manifestSha256=$hash} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage '.from-scratch-beta-owned.json') -Encoding UTF8
        [IO.Directory]::Move($stage,$target)
        $stage = $null
    }
    [void](New-Item -ItemType Directory -Path $DesktopDirectory -Force)
    $temporaryShortcut = Join-Path $DesktopDirectory ('FromScratchBeta-' + [Guid]::NewGuid().ToString('N') + '.lnk')
    $link = Get-BetaShortcut $temporaryShortcut
    try {
        $link.TargetPath = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
        $link.Arguments = '-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + (Join-Path $target 'Launch.ps1') + '"'
        $link.WorkingDirectory = $target
        $link.Description = 'From Scratch Beta - unreviewed developer client'
        $link.Save()
    } finally { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($link) }
    if (Test-Path -LiteralPath $shortcut) { [IO.File]::Replace($temporaryShortcut,$shortcut,[NullString]::Value) } else { [IO.File]::Move($temporaryShortcut,$shortcut) }
    Write-Host ('Installed From Scratch Beta ' + $manifest.version + '. Double-click From Scratch Beta on your desktop.')
    Write-Host 'Use RuneLite''s Jagex Accounts panel to sign in. The installer never asks for credentials.'
    Write-Host 'Updates keep earlier program versions. Uninstall.cmd in a version folder removes only that version; progression is preserved.'
    [pscustomobject]@{Directory=$target;Shortcut=$shortcut;Version=$manifest.version}
} finally {
    if ($stage -and (Test-Path -LiteralPath $stage)) {
        $verifiedStage = Get-BetaPath $InstallRoot ('versions/' + (Split-Path $stage -Leaf))
        if ($verifiedStage -eq $stage -and (Split-Path $stage -Leaf) -match '^staging-[a-f0-9]{32}$') { Remove-Item -LiteralPath $verifiedStage -Recurse -Force }
    }
    if ($temporaryShortcut -and (Test-Path -LiteralPath $temporaryShortcut)) { Remove-Item -LiteralPath $temporaryShortcut -Force }
}
