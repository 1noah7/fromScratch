[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$media=Join-Path (Split-Path $PSScriptRoot) 'docs/media'
[void](New-Item -ItemType Directory -Path $media -Force)
$bitmap=[Drawing.Bitmap]::new(1280,640)
$g=[Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
$svg=[Collections.Generic.List[string]]::new()
$svg.Add('<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="640" viewBox="0 0 1280 640" role="img" aria-labelledby="title desc"><title id="title">From Scratch</title><desc id="desc">Original title artwork: a purple bank chest and two contrasting counters. Not a gameplay screenshot.</desc>')
function Art-Rect($x,$y,$w,$h,$color) {
    $b=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color));try{$g.FillRectangle($b,$x,$y,$w,$h)}finally{$b.Dispose()}
    $svg.Add(('<rect x="{0}" y="{1}" width="{2}" height="{3}" fill="{4}"/>' -f $x,$y,$w,$h,$color))
}
function Art-Text($x,$y,$size,$text,$color,[switch]$Bold) {
    $style=if($Bold){[Drawing.FontStyle]::Bold}else{[Drawing.FontStyle]::Regular}
    $font=[Drawing.Font]::new('Segoe UI',[single]$size,$style,[Drawing.GraphicsUnit]::Pixel);$brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    try{$g.DrawString($text,$font,$brush,[single]$x,[single]$y)}finally{$font.Dispose();$brush.Dispose()}
    $weight=if($Bold){700}else{400}
    $svg.Add(('<text x="{0}" y="{1}" dominant-baseline="text-before-edge" font-family="Segoe UI,Arial,sans-serif" font-size="{2}" font-weight="{3}" fill="{4}">{5}</text>' -f $x,$y,$size,$weight,$color,[Security.SecurityElement]::Escape($text)))
}
function Art-Coin($x,$y,$w,$h,$color) {
    $b=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color));try{$g.FillEllipse($b,$x,$y,$w,$h)}finally{$b.Dispose()}
    $svg.Add(('<ellipse cx="{0}" cy="{1}" rx="{2}" ry="{3}" fill="{4}"/>' -f ($x+$w/2),($y+$h/2),($w/2),($h/2),$color))
}
try {
    Art-Rect 0 0 1280 640 '#101218';Art-Rect 0 0 1280 8 '#8161B4'
    Art-Rect 56 60 5 408 '#8161B4';Art-Text 80 65 19 'AN OLD SCHOOL RUNESCAPE CHALLENGE' '#B8A3D6' -Bold
    Art-Text 75 112 83 'FROM' '#F1E8D7' -Bold;Art-Text 75 198 83 'SCRATCH' '#F1E8D7' -Bold
    Art-Text 81 318 28 'Your bank is full.' '#E0D5C1';Art-Text 81 358 28 'Your adventure starts at zero.' '#1DFF00'
    Art-Text 82 427 19 'KEEP YOUR CHARACTER. EARN YOUR COMEBACK.' '#A0A0A0' -Bold
    Art-Rect 786 208 416 260 '#221D2D';Art-Rect 802 224 384 228 '#594072';Art-Rect 815 239 358 197 '#34263F'
    Art-Rect 786 177 416 39 '#8161B4';Art-Rect 802 165 384 16 '#B69AD9';Art-Rect 983 203 36 63 '#C3AE78';Art-Rect 995 221 12 24 '#302732'
    foreach($pair in @(@(844,342),@(922,365),@(1050,336))) {for($i=0;$i -lt 3;$i++){Art-Coin $pair[0] ($pair[1]-$i*12) 69 31 '#684888';Art-Coin ($pair[0]+3) ($pair[1]-$i*12) 63 19 '#AE8BD7'}}
    Art-Text 827 257 25 '110' '#A0A0A0' -Bold;Art-Text 1113 398 31 '10' '#1DFF00' -Bold
    Art-Rect 80 513 1120 2 '#393041';Art-Text 80 541 28 'YOUR BANK. TWO REALITIES.' '#F1E8D7' -Bold
    Art-Text 82 590 16 'Original title artwork  /  Independent community plugin  /  Not Plugin Hub approved' '#A0A0A0'
    Art-Text 1050 550 17 'Pilots r kwl' '#B8A3D6'
    $svg.Add('</svg>')
    [IO.File]::WriteAllText((Join-Path $media 'from-scratch-hero.svg'),($svg -join "`n"),[Text.UTF8Encoding]::new($false))
    $bitmap.Save((Join-Path $media 'from-scratch-hero.png'),[Drawing.Imaging.ImageFormat]::Png)
} finally {$g.Dispose();$bitmap.Dispose()}
Write-Host 'Original SVG and 1280x640 PNG generated. No gameplay screenshot was fabricated.'
