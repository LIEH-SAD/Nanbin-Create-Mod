<#
.SYNOPSIS
  Compare the Java class inventory of the MTR 4.0 source tree against this (MTR 3.6.3) tree.

.DESCRIPTION
  Source of truth for the source tree:  <Source>\fabric\src\main\java
  Source of truth for this tree:        <Target>\common|fabric|forge\src\main\java

  Paths are compared relative to the "src/main/java" root. Because the port intentionally
  renames/relocates a few classes, known moves are read from tools/parity/known-moves.txt
  (lines of the form `<source/relative/Path.java>=<target/relative/Path.java>`, `#` starts a comment).

.EXAMPLE
  .\tools\parity\classes.ps1
  .\tools\parity\classes.ps1 -Strict   # exit code 1 when anything is still unmapped
#>
[CmdletBinding()]
param(
    [string]$Source = 'D:\NCM+mtr4.0\Nanbin',
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

function Get-JavaInventory([string]$projectDir, [string[]]$roots) {
    $rootRel = 'src\main\java'
    $seen = @{}
    foreach ($branch in $roots) {
        $base = Join-Path $projectDir (Join-Path $branch $rootRel)
        if (-not (Test-Path $base)) { continue }
        Get-ChildItem $base -Recurse -File -Filter *.java | ForEach-Object {
            $rel = $_.FullName.Substring($base.Length + 1).Replace('\', '/')
            $seen[$rel] = $true
        }
    }
    return $seen.Keys | Sort-Object
}

$moves = @{}
$movesFile = Join-Path $PSScriptRoot 'known-moves.txt'
if (Test-Path $movesFile) {
    Get-Content $movesFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $parts = $line -split '=', 2
        if ($parts.Count -eq 2) { $moves[$parts[0].Trim()] = $parts[1].Trim() }
    }
}

$skip = @{}
$skipFile = Join-Path $PSScriptRoot 'known-skip.txt'
if (Test-Path $skipFile) {
    Get-Content $skipFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $skip[$line] = $true
    }
}

$sourceClasses = Get-JavaInventory $Source @('fabric')
$targetClasses = Get-JavaInventory $Target @('common', 'fabric', 'forge')

$targetSet = @{}
foreach ($c in $targetClasses) { $targetSet[$c] = $true }

$missing = New-Object System.Collections.ArrayList
$mapped = New-Object System.Collections.ArrayList
$skipped = 0
foreach ($c in $sourceClasses) {
    if ($targetSet.ContainsKey($c)) { continue }
    if ($skip.ContainsKey($c)) { $skipped++; continue }
    if ($moves.ContainsKey($c)) {
        $dest = $moves[$c]
        if ($targetSet.ContainsKey($dest)) { [void]$mapped.Add("$c => $dest"); continue }
        [void]$missing.Add("$c => $dest (move target missing)")
        continue
    }
    [void]$missing.Add($c)
}

$sourceSet = @{}
foreach ($c in $sourceClasses) { $sourceSet[$c] = $true }
$mappedTargets = @{}
foreach ($v in $moves.Values) { $mappedTargets[$v] = $true }
$targetOnly = $targetClasses | Where-Object { -not $sourceSet.ContainsKey($_) -and -not $mappedTargets.ContainsKey($_) }

Write-Host ("SOURCE classes : {0}" -f $sourceClasses.Count)
Write-Host ("TARGET classes : {0}" -f $targetClasses.Count)
Write-Host ("Mapped moves   : {0}" -f $mapped.Count)
Write-Host ("Intentionally skipped: {0}" -f $skipped)
Write-Host ("MISSING        : {0}" -f $missing.Count)
Write-Host ("TARGET-only    : {0}" -f $targetOnly.Count)
Write-Host ''

if ($missing.Count -gt 0) {
    Write-Host '--- MISSING in target ---'
    $missing | Sort-Object | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
}
if ($targetOnly.Count -gt 0) {
    Write-Host '--- TARGET-only (review: rename / obsolete / loader-specific) ---'
    $targetOnly | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
}

if ($Strict -and $missing.Count -gt 0) { exit 1 }
exit 0
