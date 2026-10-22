[CmdletBinding()]
param([switch]$CheckOnly, [switch]$Help)
. (Join-Path $PSScriptRoot 'Common.ps1')
try {
    $manifest = Read-BetaPackage $PSScriptRoot
    $java = Find-BetaJava
    $classpath = Join-Path $PSScriptRoot 'lib/*'
    if ($CheckOnly) { [pscustomobject]@{Version=$manifest.version;RuneLite=$manifest.runeLiteVersion;JavaMajor=11;FilesVerified=@($manifest.files).Count;MainClass=$manifest.mainClass}; return }
    if ($Help) { & $java '-ea' '-cp' $classpath $manifest.mainClass '--help'; if ($LASTEXITCODE -ne 0) { throw 'The packaged Java launcher failed.' }; return }
    $javaw = Join-Path (Split-Path $java) 'javaw.exe'
    if (-not (Test-Path -LiteralPath $javaw)) { throw 'The Java 11 installation is incomplete (javaw.exe missing).' }
    # Standard OS launch only. Authentication is performed by the unmodified RuneLite client.
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $javaw
    $start.Arguments = '-ea -cp "' + $classpath + '" ' + $manifest.mainClass
    $start.WorkingDirectory = $PSScriptRoot
    $start.UseShellExecute = $false
    $child = [Diagnostics.Process]::Start($start)
    if ($null -eq $child) { throw 'Java could not be started.' }
    $child.Dispose()
} catch {
    if ($CheckOnly -or $Help) { throw }
    Add-Type -AssemblyName System.Windows.Forms
    [void][Windows.Forms.MessageBox]::Show($_.Exception.Message,'From Scratch Beta could not start','OK','Error')
    exit 1
}
