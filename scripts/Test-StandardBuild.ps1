[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$work=Join-Path $root 'build/standard-check'
[void](New-Item -ItemType Directory -Path (Join-Path $work 'src') -Force)
$headers=@{'User-Agent'='FromScratch-standard-build-verification'}
$revision=(Invoke-RestMethod 'https://api.github.com/repos/runelite/plugin-hub-tooling/commits/master' -Headers $headers -TimeoutSec 20).sha
if($revision -notmatch '^[a-f0-9]{40}$'){throw 'Cannot verify official tooling revision.'}
$version=(Invoke-WebRequest 'https://raw.githubusercontent.com/runelite/plugin-hub/master/runelite.version' -UseBasicParsing -TimeoutSec 20).Content.Trim()
if($version -notmatch '^\d+\.\d+\.\d+$'){throw 'Cannot verify official Hub RuneLite version.'}
$base='https://raw.githubusercontent.com/runelite/plugin-hub-tooling/'+$revision+'/package/src/main/resources/net/runelite/pluginhub/packager/'
foreach($pair in @(@('standard-build.gradle','build.gradle'),@('standard-settings.gradle','settings.gradle'))){Invoke-WebRequest ($base+$pair[0]) -UseBasicParsing -TimeoutSec 20 -OutFile (Join-Path $work $pair[1])}
Copy-Item -LiteralPath (Join-Path $root 'src/main') -Destination (Join-Path $work 'src') -Recurse -Force
# Hub supplies its client version externally; keep its standard build file byte-for-byte.
$init="allprojects { configurations.configureEach { resolutionStrategy.eachDependency { if (requested.group == 'net.runelite' && requested.name == 'client') { useVersion '$version' } } } }"
[IO.File]::WriteAllText((Join-Path $work 'hub-version.gradle'),$init,[Text.UTF8Encoding]::new($false))
& (Join-Path $root 'gradlew.bat') '-p' $work '--init-script' (Join-Path $work 'hub-version.gradle') 'jar' '--console=plain'
if($LASTEXITCODE -ne 0){throw 'Official standard-template build failed.'}
[ordered]@{toolingRevision=$revision;runeLiteVersion=$version;templateSha256=(Get-FileHash -LiteralPath (Join-Path $work 'build.gradle')).Hash.ToLowerInvariant();status='local-windows-standard-template-compile-passed';scope='Official standard template plus externally supplied Hub client version. Not a hosted Plugin Hub review.'} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $work 'evidence.json') -Encoding UTF8
