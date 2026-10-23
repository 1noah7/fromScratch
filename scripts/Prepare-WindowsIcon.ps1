[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$project = Split-Path $PSScriptRoot
$source = [Drawing.Image]::FromFile((Join-Path $project 'icon.png'))
$output = Join-Path $project 'beta/windows-native/FromScratch.ico'
$frames = @()
try {
    foreach ($size in @(16,32,48,64,256)) {
        $bitmap = New-Object Drawing.Bitmap($size,$size,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.DrawImage($source,[Drawing.Rectangle]::new(0,0,$size,$size),0,0,$source.Width,$source.Height,[Drawing.GraphicsUnit]::Pixel)
            } finally { $graphics.Dispose() }
            $stream = New-Object IO.MemoryStream
            try { $bitmap.Save($stream,[Drawing.Imaging.ImageFormat]::Png); $frames += [pscustomobject]@{size=$size;bytes=$stream.ToArray()} }
            finally { $stream.Dispose() }
        } finally { $bitmap.Dispose() }
    }
} finally { $source.Dispose() }
$file = [IO.File]::Create($output); $writer = New-Object IO.BinaryWriter($file)
try {
    $writer.Write([uint16]0);$writer.Write([uint16]1);$writer.Write([uint16]$frames.Count)
    $offset=6+16*$frames.Count
    foreach ($frame in $frames) {
        $dimension=if($frame.size -eq 256){0}else{$frame.size}
        $writer.Write([byte]$dimension);$writer.Write([byte]$dimension);$writer.Write([byte]0);$writer.Write([byte]0)
        $writer.Write([uint16]1);$writer.Write([uint16]32);$writer.Write([uint32]$frame.bytes.Length);$writer.Write([uint32]$offset)
        $offset+=$frame.bytes.Length
    }
    foreach ($frame in $frames) {$writer.Write([byte[]]$frame.bytes)}
} finally {$writer.Dispose();$file.Dispose()}
Write-Host 'FromScratch.ico: original branding, PNG-backed 16/32/48/64/256-pixel frames.'
