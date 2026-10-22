[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact='High')]
param()
. (Join-Path $PSScriptRoot 'Common.ps1')
$manifest = Read-BetaPackage $PSScriptRoot -AllowChangedFiles
$stampPath = Join-Path $PSScriptRoot '.from-scratch-beta-owned.json'
if (-not (Test-Path -LiteralPath $stampPath)) { throw 'Run this only from an installed beta version. No files were removed.' }
$stamp = Get-Content -LiteralPath $stampPath -Raw | ConvertFrom-Json
$root = [IO.Path]::GetFullPath($PSScriptRoot)
$manifestHash = (Get-FileHash -LiteralPath (Join-Path $root 'package.json') -Algorithm SHA256).Hash.ToLowerInvariant()
if ($stamp.format -ne 1 -or $stamp.directory -ne $root -or $stamp.manifestSha256 -ne $manifestHash -or -not $root.StartsWith(([IO.Path]::GetFullPath($stamp.installRoot).TrimEnd('\') + '\versions\'), [StringComparison]::OrdinalIgnoreCase)) { throw 'Installation ownership could not be verified. No files were removed.' }
$tree = @(Get-BetaTree $root) # Reject links before deleting anything; never follow them.
if (-not $PSCmdlet.ShouldProcess('From Scratch Beta ' + $manifest.version,'Remove this version''s unchanged program files and its desktop shortcut. RuneLite, credentials and progression stay untouched.')) { return }
$shortcut = Join-Path $stamp.desktop 'From Scratch Beta.lnk'
if ((Test-Path -LiteralPath $shortcut) -and (Test-BetaOwnedShortcut $shortcut $stamp.installRoot)) {
    $link = Get-BetaShortcut $shortcut
    try { $sameVersion = $link.Arguments.Contains('"' + (Join-Path $root 'Launch.ps1') + '"') } finally { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($link) }
    if ($sameVersion) { Remove-Item -LiteralPath $shortcut }
}
$helpers = @('Common.ps1','Uninstall.ps1','Uninstall.cmd')
foreach ($entry in $manifest.files | Where-Object { $_.path -notin $helpers }) {
    $file = Get-BetaPath $root $entry.path
    if (Test-Path -LiteralPath $file -PathType Leaf) {
        if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -eq $entry.sha256) { Remove-Item -LiteralPath $file } else { Write-Warning ('Preserved changed file: ' + $entry.path) }
    }
}
$remaining = @(Get-BetaTree $root | Where-Object { -not $_.PSIsContainer -and $_.Name -notin ($helpers + @('package.json','.from-scratch-beta-owned.json')) })
if ($remaining.Count -eq 0) {
    foreach ($helper in $helpers) { $file=Get-BetaPath $root $helper; $entry=$manifest.files | Where-Object {$_.path -eq $helper}; if ((Test-Path -LiteralPath $file) -and (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -eq $entry.sha256) { Remove-Item -LiteralPath $file } }
    if (@(Get-BetaTree $root | Where-Object { -not $_.PSIsContainer -and $_.Name -notin @('package.json','.from-scratch-beta-owned.json') }).Count -eq 0) { Remove-Item -LiteralPath (Join-Path $root 'package.json'),$stampPath }
}
# No recursive deletion: unknown/modified files and all progression live outside this allowlist.
Get-BetaTree $root | Where-Object {$_.PSIsContainer} | Sort-Object { $_.FullName.Length } -Descending | ForEach-Object { if (@(Get-ChildItem -LiteralPath $_.FullName -Force).Count -eq 0) { Remove-Item -LiteralPath $_.FullName } }
if (@(Get-ChildItem -LiteralPath $root -Force).Count -eq 0) { Remove-Item -LiteralPath $root }
Write-Host 'Beta program files removed where unchanged. Any modified/unrelated files were retained. Progression, backups, credentials and normal RuneLite were not touched.'
