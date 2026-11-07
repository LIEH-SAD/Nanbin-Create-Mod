<#
.SYNOPSIS
  Compare the resource inventory (models / blockstates / textures / recipes / lang keys / js)
  of the MTR 4.0 source tree against this tree.

.DESCRIPTION
  Source resources: <Source>\fabric\src\main\resources
  Target resources: <Target>\common|fabric|forge\src\main\resources

  Lang files are compared at key level (not file level) so nesting/formatting differences
  do not produce noise.

.EXAMPLE
  .\tools\parity\resources.ps1
  .\tools\parity\resources.ps1 -Strict
#>
[CmdletBinding()]
param(
    [string]$Source = 'D:\NCM+mtr4.0\Nanbin',
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

function Get-RelativeFiles([string]$baseDir) {
    if (-not (Test-Path $baseDir)) { return @() }
    Get-ChildItem $baseDir -Recurse -File | ForEach-Object {
        $_.FullName.Substring($baseDir.Length + 1).Replace('\', '/')
    } | Sort-Object
}

function Get-Inventory([string]$projectDir, [string[]]$branches) {
    $all = @()
    foreach ($b in $branches) {
        $dir = Join-Path $projectDir (Join-Path $b 'src\main\resources')
        $all += Get-RelativeFiles $dir
    }
    return $all | Sort-Object -Unique
}

$src = Get-Inventory $Source @('fabric')
$tgt = Get-Inventory $Target @('common', 'fabric', 'forge')

$groups = @(
    @{ Name = 'assets/nanbin/models';      Filter = '^assets/nanbin/models/' },
    @{ Name = 'assets/nanbin/blockstates'; Filter = '^assets/nanbin/blockstates/' },
    @{ Name = 'assets/nanbin/textures';    Filter = '^assets/nanbin/textures/' },
    @{ Name = 'assets/nanbin/js';          Filter = '^assets/nanbin/js/' },
    @{ Name = 'assets/nanbin/pids';        Filter = '^assets/nanbin/pids/' },
    @{ Name = 'assets/nanbin/contributors';Filter = '^assets/nanbin/contributors/' },
    @{ Name = 'assets/nanbin/font';        Filter = '^assets/nanbin/font/' },
    @{ Name = 'assets/nanbin (root)';      Filter = '^assets/nanbin/[^/]+$' },
    @{ Name = 'data/nanbin/recipes';       Filter = '^data/nanbin/recipes/' },
    @{ Name = 'data (other)';              Filter = '^data/' },
    @{ Name = 'assets/mtr';                Filter = '^assets/mtr/' }
)

$failed = $false
foreach ($g in $groups) {
    $s = $src | Where-Object { $_ -match $g.Filter }
    $t = $tgt | Where-Object { $_ -match $g.Filter }
    $missing = Compare-Object $s $t | Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject
    $extra = Compare-Object $s $t | Where-Object SideIndicator -eq '=>' | ForEach-Object InputObject
    $status = if ($missing.Count -eq 0) { 'OK' } else { 'MISSING' }
    if ($missing.Count -gt 0) { $failed = $true }
    Write-Host ("{0,-28} src={1,4} tgt={2,4} missing={3,3} extra={4,3}  {5}" -f $g.Name, $s.Count, $t.Count, @($missing).Count, @($extra).Count, $status)
    if ($missing.Count -gt 0 -and $missing.Count -le 40) {
        $missing | ForEach-Object { Write-Host "    + $_" }
    }
}

Write-Host ''
Write-Host '--- lang keys ---'
$langDirSrc = Join-Path $Source 'fabric\src\main\resources\assets\nanbin\lang'
$langDirTgt = Join-Path $Target 'common\src\main\resources\assets\nanbin\lang'
$langs = @()
if (Test-Path $langDirSrc) { $langs += Get-ChildItem $langDirSrc -File -Filter *.json | ForEach-Object { $_.Name } }
$langs = $langs | Sort-Object -Unique
foreach ($lang in $langs) {
    $sp = Join-Path $langDirSrc $lang
    $tp = Join-Path $langDirTgt $lang
    $sKeys = @()
    if (Test-Path $sp) { $sKeys = [System.IO.File]::ReadAllLines($sp) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $matches[1] } }
    $tKeys = @()
    if (Test-Path $tp) { $tKeys = [System.IO.File]::ReadAllLines($tp) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $matches[1] } }
    $missing = Compare-Object $sKeys $tKeys | Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject
    $extra = Compare-Object $sKeys $tKeys | Where-Object SideIndicator -eq '=>' | ForEach-Object InputObject
    if ($missing.Count -gt 0) { $failed = $true }
    Write-Host ("{0,-12} src={1,4} tgt={2,4} missing={3,4} extra={4,3}" -f $lang, $sKeys.Count, $tKeys.Count, @($missing).Count, @($extra).Count)
    if ($missing.Count -gt 0 -and $missing.Count -le 15) {
        $missing | ForEach-Object { Write-Host "    + $_" }
    }
}

if ($Strict -and $failed) { exit 1 }
exit 0
