<#
.SYNOPSIS
  P7 resource sync: MTR 4.0 source resources -> this tree (additive, overwrite same-named).

.DESCRIPTION
  Source: <Source>\fabric\src\main\resources
  Target: <Target>\common\src\main\resources

  Rules (see docs\PORTING_MTR3.md):
    * Additive only: files that exist only in the target are NEVER deleted.
    * Same-named files are overwritten with the source version (source fixed texture
      resolutions and models).
    * Binary files (png/ogg/ttf/otf) are copied byte-for-byte with Copy-Item -Force.
    * Text files are read/written with [IO.File]::ReadAllText/WriteAllText + UTF8
      without BOM (never shell redirection) so Chinese text is preserved.
    * Assets\nanbin\crt_signs.json is absent from source HEAD (deleted in 9274aba) but
      still referenced by CRTPresetCatalog -> restored from commit 4834a43.
    * Protected files (fabric.mod.json, nanbin.mixins.json, mods.toml, data/minecraft
      tags, assets/mtr/**, msd.accesswidener) are outside this script's scope.

.EXAMPLE
  .\tools\parity\sync-resources.ps1
  .\tools\parity\sync-resources.ps1 -WhatIf
#>
[CmdletBinding()]
param(
    [string]$Source = 'D:\NCM+mtr4.0\Nanbin',
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$srcRoot = Join-Path $Source 'fabric\src\main\resources'
$tgtRoot = Join-Path $Target 'common\src\main\resources'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$binaryExt = @('.png', '.ogg', '.ttf', '.otf', '.wav', '.jar')
$crtSignsCommit = '4834a43'
$crtSignsRepoPath = 'fabric/src/main/resources/assets/nanbin/crt_signs.json'

function Write-TextNoBom([string]$from, [string]$to) {
    $text = [System.IO.File]::ReadAllText($from)
    if (-not $WhatIf) {
        $dir = [System.IO.Path]::GetDirectoryName($to)
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        [System.IO.File]::WriteAllText($to, $text, $utf8NoBom)
    }
}

function Test-SameFile([string]$from, [string]$to) {
    # Byte-equal => same. Otherwise, for text files a difference caused only by the
    # source's UTF-8 BOM (which we deliberately strip) still counts as "same".
    $hb = (Get-FileHash -LiteralPath $from -Algorithm SHA256).Hash
    $ht = (Get-FileHash -LiteralPath $to -Algorithm SHA256).Hash
    if ($hb -eq $ht) { return $true }
    if ($binaryExt -contains ([System.IO.Path]::GetExtension($from).ToLowerInvariant())) { return $false }
    $tb = [System.IO.File]::ReadAllBytes($to)
    $tgtHasBom = ($tb.Length -ge 3 -and $tb[0] -eq 0xEF -and $tb[1] -eq 0xBB -and $tb[2] -eq 0xBF)
    if ($tgtHasBom) { return $false }
    $srcText = [System.IO.File]::ReadAllText($from)
    $tgtText = [System.IO.File]::ReadAllText($to)
    return ($srcText -ceq $tgtText)
}

function Sync-Tree([string]$rel) {
    $sd = Join-Path $srcRoot $rel
    $dd = Join-Path $tgtRoot $rel
    if (-not (Test-Path -LiteralPath $sd)) { Write-Host ("  SKIP {0} (source missing)" -f $rel); return }
    $added = 0; $overwritten = 0; $identical = 0
    foreach ($f in (Get-ChildItem -LiteralPath $sd -Recurse -File)) {
        $r = $f.FullName.Substring($sd.Length + 1)
        $to = Join-Path $dd $r
        $exists = Test-Path -LiteralPath $to
        if ($exists -and (Test-SameFile $f.FullName $to)) { $identical++; continue }
        if (-not $WhatIf) {
            $dir = [System.IO.Path]::GetDirectoryName($to)
            if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            if ($binaryExt -contains $f.Extension.ToLowerInvariant()) {
                Copy-Item -LiteralPath $f.FullName -Destination $to -Force
            }
            else {
                $text = [System.IO.File]::ReadAllText($f.FullName)
                [System.IO.File]::WriteAllText($to, $text, $utf8NoBom)
            }
        }
        if ($exists) { $overwritten++ } else { $added++ }
    }
    Write-Host ("  {0,-28} added={1,3} overwritten={2,3} identical={3,3}" -f $rel, $added, $overwritten, $identical)
}

Write-Host '=== asset trees (additive + overwrite) ==='
foreach ($g in @(
        'assets\nanbin\models',
        'assets\nanbin\blockstates',
        'assets\nanbin\textures',
        'assets\nanbin\contributors',
        'assets\nanbin\font',
        'assets\nanbin\pids',
        'assets\nanbin\sounds',
        'data\nanbin\recipes')) {
    Sync-Tree $g
}

Write-Host '=== single files under assets\nanbin ==='
foreach ($rel in @(
        'assets\nanbin\logo.png',
        'assets\nanbin\sounds.json',
        'assets\nanbin\crt_js_signs_config.json',
        'assets\nanbin\js\crt_station_entrance.js')) {
    $from = Join-Path $srcRoot $rel
    $to = Join-Path $tgtRoot $rel
    if (-not (Test-Path -LiteralPath $from)) { Write-Host ("  SKIP {0} (source missing)" -f $rel); continue }
    $exists = Test-Path -LiteralPath $to
    if ($exists) {
        $hb = (Get-FileHash -LiteralPath $from -Algorithm SHA256).Hash
        $ht = (Get-FileHash -LiteralPath $to -Algorithm SHA256).Hash
        if ($hb -eq $ht) { Write-Host ("  identical  {0}" -f $rel); continue }
    }
    if (-not $WhatIf) {
        $dir = [System.IO.Path]::GetDirectoryName($to)
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        if ($binaryExt -contains ([System.IO.Path]::GetExtension($from).ToLowerInvariant())) {
            Copy-Item -LiteralPath $from -Destination $to -Force
        }
        else {
            $text = [System.IO.File]::ReadAllText($from)
            [System.IO.File]::WriteAllText($to, $text, $utf8NoBom)
        }
    }
    $state = if ($exists) { 'overwritten' } else { 'added' }
    Write-Host ("  {0,-11} {1}" -f $state, $rel)
}

Write-Host '=== lang (9 locales; target-only keys would be reported, none exist) ==='
$srcLang = Join-Path $srcRoot 'assets\nanbin\lang'
$tgtLang = Join-Path $tgtRoot 'assets\nanbin\lang'
$getKeys = { param($p) if (Test-Path -LiteralPath $p) { [System.IO.File]::ReadAllLines($p) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $matches[1] } } else { @() } }
foreach ($f in (Get-ChildItem -LiteralPath $srcLang -File -Filter *.json | Sort-Object Name)) {
    $sp = Join-Path $srcLang $f.Name
    $tp = Join-Path $tgtLang $f.Name
    $sk = @(& $getKeys $sp)
    $tk = @(& $getKeys $tp)
    $extra = @(Compare-Object $sk $tk | Where-Object SideIndicator -eq '=>' | ForEach-Object InputObject)
    if ($extra.Count -gt 0) {
        Write-Host ("  {0,-12} MERGE NEEDED - target-only keys: {1}" -f $f.Name, ($extra -join ', '))
        continue
    }
    if ((Test-Path -LiteralPath $tp) -and (Test-SameFile $sp $tp)) {
        Write-Host ("  {0,-12} identical (keys {1})" -f $f.Name, $sk.Count)
        continue
    }
    Write-TextNoBom $sp $tp
    Write-Host ("  {0,-12} overwritten  keys {1} -> {2}" -f $f.Name, $tk.Count, $sk.Count)
}

Write-Host '=== crt_signs.json (restore from git history, byte-exact) ==='
$signsOut = Join-Path $tgtRoot 'assets\nanbin\crt_signs.json'
$tmp = Join-Path $env:TEMP ('crt_signs_' + [guid]::NewGuid().ToString('N') + '.json')
$cmdLine = 'git -C "' + $Source + '" show "' + $crtSignsCommit + ':' + $crtSignsRepoPath + '" > "' + $tmp + '"'
& cmd.exe /c $cmdLine
if ($LASTEXITCODE -ne 0) { throw "git show $crtSignsCommit failed (exit $LASTEXITCODE)" }
$bytes = [System.IO.File]::ReadAllBytes($tmp)
$strictUtf8 = New-Object System.Text.UTF8Encoding($false, $true)
$text = $strictUtf8.GetString($bytes)   # validates UTF-8
if (-not $WhatIf) {
    [System.IO.File]::WriteAllText($signsOut, $text, $utf8NoBom)
}
Remove-Item -LiteralPath $tmp -Force
$parsed = $text | ConvertFrom-Json
Write-Host ("  written {0} bytes; signs = {1}" -f $bytes.Length, @($parsed.signs).Count)

Write-Host '=== root files / docs ==='
foreach ($rel in @('wthit_plugins.json', 'JS_API.md', 'LICENSE')) {
    $from = Join-Path $Source $rel
    if (-not (Test-Path -LiteralPath $from)) { Write-Host ("  SKIP {0} (source missing)" -f $rel); continue }
    Write-TextNoBom $from (Join-Path $Target $rel)
    Write-Host ("  synced {0}" -f $rel)
}
$notices = Join-Path $Source 'docs\THIRD_PARTY_NOTICES.md'
if (Test-Path -LiteralPath $notices) {
    Write-TextNoBom $notices (Join-Path $Target 'THIRD_PARTY_NOTICES.md')
    Write-Host '  synced THIRD_PARTY_NOTICES.md (source keeps it under docs/)'
}
$srcDocs = Join-Path $Source 'docs'
$tgtDocs = Join-Path $Target 'docs'
foreach ($f in (Get-ChildItem -LiteralPath $srcDocs -Recurse -File)) {
    $rel = $f.FullName.Substring($srcDocs.Length + 1)
    $to = Join-Path $tgtDocs $rel
    if (Test-Path -LiteralPath $to) { Write-Host ("  KEEP {0} (already present, not overwritten)" -f $rel); continue }
    if (-not $WhatIf) {
        $dir = [System.IO.Path]::GetDirectoryName($to)
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        Copy-Item -LiteralPath $f.FullName -Destination $to -Force
    }
    Write-Host ("  added docs\{0}" -f $rel)
}
Write-Host 'done.'
