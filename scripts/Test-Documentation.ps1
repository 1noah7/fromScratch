[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$errors = New-Object 'System.Collections.Generic.List[string]'
$links = 0; $images = 0
$anchors = @{}
function Get-DocAnchors([string]$Path) {
    if ($anchors.ContainsKey($Path)) { return $anchors[$Path] }
    $headings = [IO.File]::ReadAllText($Path)
    $headings = [regex]::Replace($headings, '(?ms)^```.*?^```\s*$', '')
    $found = @{}; $duplicates = @{}
    foreach ($heading in [regex]::Matches($headings, '(?m)^#{1,6}\s+(?<title>[^\r\n]+)')) {
        $slug = $heading.Groups['title'].Value.Trim().TrimEnd('#').Trim().ToLowerInvariant()
        $slug = [regex]::Replace($slug, '[^\p{L}\p{N}_\- ]', '').Replace(' ', '-')
        $n = if ($duplicates.ContainsKey($slug)) { $duplicates[$slug] } else { 0 }
        $duplicates[$slug] = $n + 1
        if ($n -gt 0) { $slug += '-' + $n }
        $found[$slug] = $true
    }
    $anchors[$Path] = $found; return $found
}
$files = @((Get-ChildItem -LiteralPath $project -Filter '*.md' -File)) + @((Get-ChildItem -LiteralPath (Join-Path $project 'docs') -Filter '*.md' -File -Recurse)) + @((Get-ChildItem -LiteralPath (Join-Path $project '.github') -Filter '*.md' -File -Recurse))
foreach ($file in $files) {
    $text = [IO.File]::ReadAllText($file.FullName)
    # Fence contents are examples, not rendered links.
    $text = [regex]::Replace($text, '(?ms)^```.*?^```\s*$', '')
    $targets = @([regex]::Matches($text, '!?\[[^\]\r\n]*\]\((?<url>[^\s\)]+)\)') | ForEach-Object {$_.Groups['url'].Value})
    $targets += @([regex]::Matches($text, '<img\b[^>]*\bsrc="(?<url>[^"]+)"') | ForEach-Object {$_.Groups['url'].Value})
    foreach ($url in $targets) {
        if ($url -match '^(https?:|mailto:)') { continue }
        $parts = $url -split '#',2
        $local = [Uri]::UnescapeDataString($parts[0])
        $target = if ($local) { [IO.Path]::GetFullPath((Join-Path $file.DirectoryName $local)) } else { $file.FullName }
        $links++
        if (!(Test-Path -LiteralPath $target)) { $errors.Add($file.FullName.Substring($project.Length+1) + ': missing ' + $url); continue }
        if ($parts.Length -eq 2 -and $target.EndsWith('.md') -and !(Get-DocAnchors $target).ContainsKey([Uri]::UnescapeDataString($parts[1]))) {
            $errors.Add($file.FullName.Substring($project.Length+1) + ': missing heading ' + $url)
        }
    }
}
Add-Type -AssemblyName System.Drawing
foreach ($file in Get-ChildItem -LiteralPath (Join-Path $project 'docs/media') -Filter '*.png' -File) {
    $stream = [IO.File]::OpenRead($file.FullName)
    try {
        $signature = New-Object byte[] 8; [void]$stream.Read($signature,0,8); $stream.Position=0
        if ([BitConverter]::ToString($signature) -ne '89-50-4E-47-0D-0A-1A-0A') { throw 'Not a PNG' }
        $image = [Drawing.Image]::FromStream($stream)
        try { if ($image.Width -lt 300 -or $image.Height -lt 200) { throw 'Preview too small to read' }; Write-Host ($file.Name + ': ' + $image.Width + 'x' + $image.Height + ', ' + $file.Length + ' bytes'); $images++ }
        finally { $image.Dispose() }
    } finally { $stream.Dispose() }
}
$readme = [IO.File]::ReadAllText((Join-Path $project 'README.md'))
if ($readme -match '[\u2013\u2014]' -or $readme -match '(?i)\b(IntelliJ|Gradle|Java|API|compil(?:e|ation))\b') { $errors.Add('README has unapproved technical wording or dash separators.') }
if ($errors.Count) { throw ($errors -join "`n") }
Write-Host ('PASS ' + $files.Count + ' Markdown files, ' + $links + ' local references, ' + $images + ' PNG assets; README style check passed.')
