<#
.SYNOPSIS
  Cross-check registered block/item ids against resources (blockstates, models, lang) and recipes.

.DESCRIPTION
  Reads the ids registered in Blocks.java / Items.java, then verifies:
    * assets/nanbin/blockstates/<id>.json
    * assets/nanbin/models/block/<id>.json  (or <id>_*.json for multi-model blocks)
    * assets/nanbin/models/item/<id>.json
    * lang keys block.nanbin.<id> / item.nanbin.<id>
  Then parses data/nanbin/recipes/*.json and reports references to nanbin:<id> that are
  neither a registered block id nor a registered item id.

.EXAMPLE
  .\tools\parity\registry.ps1
#>
[CmdletBinding()]
param(
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

$javaRoot = Join-Path $Target 'common\src\main\java\com\Nanbin'
$resRoot = Join-Path $Target 'common\src\main\resources'
$assets = Join-Path $resRoot 'assets\nanbin'

function Get-RegisteredIds([string]$file, [string]$fallbackPattern = 'register\(\s*"([a-z0-9_]+)"') {
    if (-not (Test-Path $file)) { return @() }
    $text = [System.IO.File]::ReadAllText($file)
    $ids = @()
    foreach ($m in [regex]::Matches($text, $fallbackPattern)) { $ids += $m.Groups[1].Value }
    return $ids | Sort-Object -Unique
}

$blockIds = Get-RegisteredIds (Join-Path $javaRoot 'Blocks\Blocks.java')
$itemIds = Get-RegisteredIds (Join-Path $javaRoot 'Items\Items.java')

# 1.20.1 原版方块/物品也可能出现在配方里，这里只校验 nanbin 命名空间
$known = @{}
foreach ($id in $blockIds) { $known[$id] = $true }
foreach ($id in $itemIds) { $known[$id] = $true }

Write-Host ("Registered blocks: {0}" -f $blockIds.Count)
Write-Host ("Registered items : {0}" -f $itemIds.Count)
Write-Host ''

$problems = New-Object System.Collections.ArrayList

Write-Host '--- resource completeness per block id ---'
foreach ($id in $blockIds) {
    $missingHere = @()
    if (-not (Test-Path (Join-Path $assets "blockstates\$id.json"))) { $missingHere += 'blockstate' }
    $modelHits = @(Get-ChildItem (Join-Path $assets 'models\block') -File -Filter "$id.json" -ErrorAction SilentlyContinue) +
                 @(Get-ChildItem (Join-Path $assets 'models\block') -File -Filter "$id`_*.json" -ErrorAction SilentlyContinue)
    if ($modelHits.Count -eq 0) { $missingHere += 'block-model' }
    if (-not (Test-Path (Join-Path $assets "models\item\$id.json"))) { $missingHere += 'item-model' }
    if ($missingHere.Count -gt 0) {
        [void]$problems.Add("$id : $($missingHere -join ', ')")
    }
}
if ($problems.Count -eq 0) { Write-Host '  all registered blocks have blockstate + model + item model' }
else { $problems | ForEach-Object { Write-Host "  MISSING  $_" } }

Write-Host ''
Write-Host '--- lang keys ---'
$langDir = Join-Path $assets 'lang'
$langFiles = @()
if (Test-Path $langDir) { $langFiles = Get-ChildItem $langDir -File -Filter *.json | ForEach-Object { $_.Name } }
foreach ($lang in $langFiles) {
    $path = Join-Path $langDir $lang
    $keys = @{}
    [System.IO.File]::ReadAllLines($path) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $keys[$matches[1]] = $true }
    $missingKeys = New-Object System.Collections.ArrayList
    foreach ($id in $blockIds) {
        if (-not $keys.ContainsKey("block.nanbin.$id")) { [void]$missingKeys.Add("block.nanbin.$id") }
    }
    foreach ($id in $itemIds) {
        if (-not $keys.ContainsKey("item.nanbin.$id")) { [void]$missingKeys.Add("item.nanbin.$id") }
    }
    if ($missingKeys.Count -gt 0) {
        Write-Host ("  {0,-12} missing {1} key(s), first 8: {2}" -f $lang, $missingKeys.Count, (($missingKeys | Select-Object -First 8) -join ', '))
        $problems.Add("lang $lang missing $($missingKeys.Count)")
    } else {
        Write-Host ("  {0,-12} OK" -f $lang)
    }
}

Write-Host ''
Write-Host '--- recipe references ---'
$recipeDir = Join-Path $resRoot 'data\nanbin\recipes'
$unknownRefs = New-Object System.Collections.ArrayList
if (Test-Path $recipeDir) {
    foreach ($recipe in Get-ChildItem $recipeDir -File -Filter *.json) {
        $raw = [System.IO.File]::ReadAllText($recipe.FullName)
        foreach ($m in [regex]::Matches($raw, '"nanbin:([a-z0-9_]+)"')) {
            $ref = $m.Groups[1].Value
            if (-not $known.ContainsKey($ref)) {
                [void]$unknownRefs.Add("$($recipe.Name) -> nanbin:$ref")
            }
        }
    }
}
if ($unknownRefs.Count -eq 0) {
    Write-Host ("  {0} recipe file(s), all nanbin: references resolve" -f @(Get-ChildItem $recipeDir -File -Filter *.json -ErrorAction SilentlyContinue).Count)
} else {
    $unknownRefs | Select-Object -First 30 | ForEach-Object { Write-Host "  UNRESOLVED  $_" }
    Write-Host ("  total unresolved: {0}" -f $unknownRefs.Count)
    [void]$problems.Add("recipes unresolved: $($unknownRefs.Count)")
}

Write-Host ''
if ($problems.Count -eq 0) {
    Write-Host 'RESULT: OK'
    exit 0
}
Write-Host ("RESULT: {0} problem line(s)" -f $problems.Count)
if ($Strict) { exit 1 }
exit 0
